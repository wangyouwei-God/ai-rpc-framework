package com.aicore.rpc.server;

import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import com.aicore.rpc.metrics.MetricsManager;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.model.RpcResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.Method;
import java.util.Map;

public class RpcServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final Map<String, Object> serviceRegistry;
    private final MeterRegistry meterRegistry;

    public RpcServerHandler(Map<String, Object> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.meterRegistry = MetricsManager.getMeterRegistry();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        byte messageType = msg.getMessageType();

        if (messageType == ProtocolConstants.TYPE_HEARTBEAT_REQUEST) {
            RpcResponse pongResponse = new RpcResponse();
            pongResponse.setResult("PONG");
            RpcMessage responseMessage = RpcMessage.builder()
                    .serializerType(msg.getSerializerType())
                    .messageType(ProtocolConstants.TYPE_HEARTBEAT_RESPONSE)
                    .messageId(msg.getMessageId())
                    .data(pongResponse).build();
            ctx.writeAndFlush(responseMessage);
            return;
        }

        if (messageType == ProtocolConstants.TYPE_REQUEST) {
            Timer.Sample sample = Timer.start(meterRegistry);
            RpcRequest request = (RpcRequest) msg.getData();
            String serviceName = request.getClassName();
            String methodName = request.getMethodName();
            String status = "success";

            RpcMessage responseMessage = RpcMessage.builder()
                    .serializerType(msg.getSerializerType())
                    .messageType(ProtocolConstants.TYPE_RESPONSE)
                    .messageId(msg.getMessageId())
                    .build();

            RpcResponse response = new RpcResponse();
            try {
                Object service = serviceRegistry.get(serviceName);
                if (service == null) throw new RuntimeException("Service not found: " + serviceName);
                Method method = service.getClass().getMethod(methodName, request.getParamTypes());
                Object result = method.invoke(service, request.getParams());
                response.setResult(result);
            } catch (Exception e) {
                status = "error";
                response.setError(e.getCause());
            } finally {
                responseMessage.setData(response);
                sample.stop(meterRegistry.timer("rpc.server.processing", "service", serviceName, "method", methodName, "status", status));
                ctx.writeAndFlush(responseMessage);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}