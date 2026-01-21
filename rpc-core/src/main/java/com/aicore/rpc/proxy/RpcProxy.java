package com.aicore.rpc.proxy;

import com.aicore.rpc.circuitbreaker.CircuitBreaker;
import com.aicore.rpc.circuitbreaker.CircuitBreakerOpenException;
import com.aicore.rpc.circuitbreaker.CircuitBreakerRegistry;
import com.aicore.rpc.timeout.AdaptiveTimeout;
import com.aicore.rpc.timeout.AdaptiveTimeoutRegistry;
import com.aicore.rpc.client.RpcClientHandler;
import com.aicore.rpc.client.pool.ChannelPoolManager;
import com.aicore.rpc.config.ConfigManager;
import com.aicore.rpc.loadbalance.LoadBalancer;
import com.aicore.rpc.message.MessageIdGenerator;
import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.model.RpcResponse;
import com.aicore.rpc.registry.Registry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RpcProxy {

    public static <T> T create(Class<T> clazz, Registry registry, LoadBalancer loadBalancer,
            MeterRegistry meterRegistry) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[] { clazz },
                new InvocationHandlerImpl(clazz, registry, loadBalancer, meterRegistry));
    }

    private static class InvocationHandlerImpl implements InvocationHandler {
        private final Class<?> clazz;
        private final Registry registry;
        private final LoadBalancer loadBalancer;
        private final ChannelPoolManager poolManager;
        private final MeterRegistry meterRegistry;

        InvocationHandlerImpl(Class<?> clazz, Registry registry, LoadBalancer loadBalancer,
                MeterRegistry meterRegistry) {
            this.clazz = clazz;
            this.registry = registry;
            this.loadBalancer = loadBalancer;
            this.poolManager = ChannelPoolManager.getInstance();
            this.meterRegistry = meterRegistry;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Timer.Sample sample = Timer.start(meterRegistry);
            String serviceName = this.clazz.getName();
            String methodName = method.getName();
            String status = "success";
            long startTime = System.currentTimeMillis();

            // 这个 responseFuture 是整个异步流程的最终结果
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            InetSocketAddress address = null;
            CircuitBreaker circuitBreaker = null;

            try {
                List<InetSocketAddress> serviceAddresses = registry.discover(serviceName);
                if (serviceAddresses == null || serviceAddresses.isEmpty()) {
                    throw new RuntimeException("No available service provider for: " + serviceName);
                }
                address = loadBalancer.select(serviceAddresses);

                // Get or create circuit breaker for this endpoint
                String cbName = serviceName + "@" + address.getHostString() + ":" + address.getPort();
                circuitBreaker = CircuitBreakerRegistry.getInstance().getOrCreate(cbName);

                // Check if circuit breaker allows the request
                if (!circuitBreaker.allowRequest()) {
                    throw new CircuitBreakerOpenException(cbName, circuitBreaker.getState());
                }

                ChannelPool pool = poolManager.getOrCreatePool(address);
                Future<Channel> channelFuture = pool.acquire();
                final CircuitBreaker cb = circuitBreaker;
                final long callStartTime = startTime;

                // 异步地处理连接获取的结果
                channelFuture.addListener((FutureListener<Channel>) f -> {
                    if (f.isSuccess()) {
                        Channel channel = f.getNow();
                        try {
                            RpcRequest request = RpcRequest.builder()
                                    .className(serviceName)
                                    .methodName(methodName)
                                    .paramTypes(method.getParameterTypes())
                                    .params(args).build();

                            RpcMessage message = RpcMessage.builder()
                                    .serializerType(ProtocolConstants.SERIALIZER_PROTOSTUFF)
                                    .messageType(ProtocolConstants.TYPE_REQUEST)
                                    .messageId(MessageIdGenerator.nextId())
                                    .data(request).build();

                            RpcClientHandler handler = channel.pipeline().get(RpcClientHandler.class);

                            // 调用异步的 sendRequest，它返回一个 Future
                            CompletableFuture<RpcResponse> rpcCallFuture = handler.sendRequest(message, channel);

                            // 链式处理：当 rpcCallFuture 完成时，再来完成 responseFuture
                            rpcCallFuture.whenComplete((response, throwable) -> {
                                // 无论成功或失败，都释放连接回池
                                pool.release(channel);
                                long duration = System.currentTimeMillis() - callStartTime;

                                if (throwable != null) {
                                    cb.recordFailure();
                                    responseFuture.completeExceptionally(throwable);
                                } else if (response.getError() != null) {
                                    cb.recordFailure();
                                    responseFuture.complete(response);
                                } else {
                                    cb.recordSuccess(duration);
                                    responseFuture.complete(response);
                                }
                            });
                        } catch (Exception e) {
                            pool.release(channel); // 异常时也要释放连接
                            cb.recordFailure();
                            responseFuture.completeExceptionally(e);
                        }
                    } else {
                        cb.recordFailure();
                        responseFuture.completeExceptionally(f.cause());
                    }
                });

                // 主线程在这里阻塞等待最终结果
                // Use adaptive timeout or fallback to configured default
                String timeoutKey = serviceName + "@" + address.getHostString() + ":" + address.getPort();
                AdaptiveTimeout adaptiveTimeout = AdaptiveTimeoutRegistry.getInstance().getOrCreate(timeoutKey);
                int requestTimeout = adaptiveTimeout.getTimeoutSeconds();
                if (requestTimeout <= 0) {
                    requestTimeout = ConfigManager.getInt("rpc.client.request.timeout-seconds", 10);
                }
                RpcResponse response = responseFuture.get(requestTimeout, TimeUnit.SECONDS);

                // Record latency for adaptive timeout calculation
                long callDuration = System.currentTimeMillis() - startTime;
                adaptiveTimeout.recordLatency(callDuration);

                if (response.getError() != null) {
                    status = "error";
                    throw response.getError();
                }
                return response.getResult();
            } catch (CircuitBreakerOpenException e) {
                status = "circuit_open";
                throw e;
            } catch (Exception e) {
                status = "error";
                throw e;
            } finally {
                sample.stop(meterRegistry.timer("rpc.client.requests", "service", serviceName, "method", methodName,
                        "status", status));
            }
        }
    }
}