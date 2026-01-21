package com.aicore.rpc.proxy;

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

    public static <T> T create(Class<T> clazz, Registry registry, LoadBalancer loadBalancer, MeterRegistry meterRegistry) {
        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                new Class<?>[]{clazz},
                new InvocationHandlerImpl(clazz, registry, loadBalancer, meterRegistry)
        );
    }

    private static class InvocationHandlerImpl implements InvocationHandler {
        private final Class<?> clazz;
        private final Registry registry;
        private final LoadBalancer loadBalancer;
        private final ChannelPoolManager poolManager;
        private final MeterRegistry meterRegistry;

        InvocationHandlerImpl(Class<?> clazz, Registry registry, LoadBalancer loadBalancer, MeterRegistry meterRegistry) {
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

            // 这个 responseFuture 是整个异步流程的最终结果
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            try {
                List<InetSocketAddress> serviceAddresses = registry.discover(serviceName);
                if (serviceAddresses == null || serviceAddresses.isEmpty()) {
                    throw new RuntimeException("No available service provider for: " + serviceName);
                }
                InetSocketAddress address = loadBalancer.select(serviceAddresses);

                ChannelPool pool = poolManager.getOrCreatePool(address);
                Future<Channel> channelFuture = pool.acquire();

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
                                if (throwable != null) {
                                    responseFuture.completeExceptionally(throwable);
                                } else {
                                    responseFuture.complete(response);
                                }
                            });
                        } catch (Exception e) {
                            pool.release(channel); // 异常时也要释放连接
                            responseFuture.completeExceptionally(e);
                        }
                    } else {
                        responseFuture.completeExceptionally(f.cause());
                    }
                });

                // 主线程在这里阻塞等待最终结果
                int requestTimeout = ConfigManager.getInt("rpc.client.request.timeout-seconds", 10);
                RpcResponse response = responseFuture.get(requestTimeout, TimeUnit.SECONDS);

                if (response.getError() != null) {
                    status = "error";
                    throw response.getError();
                }
                return response.getResult();
            } catch (Exception e) {
                status = "error";
                throw e;
            } finally {
                sample.stop(meterRegistry.timer("rpc.client.requests", "service", serviceName, "method", methodName, "status", status));
            }
        }
    }
}