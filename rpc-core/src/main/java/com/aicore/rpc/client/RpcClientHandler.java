package com.aicore.rpc.client;

import com.aicore.rpc.message.MessageIdGenerator;
import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.model.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private final ConcurrentHashMap<Integer, CompletableFuture<RpcResponse>> pendingRPC = new ConcurrentHashMap<>();

    /**
     * 异步发送请求。
     * 此方法不阻塞，立即返回一个代表未来结果的 CompletableFuture。
     * 这是避免阻塞 Netty IO 线程的关键。
     */
    public CompletableFuture<RpcResponse> sendRequest(RpcMessage message, Channel channel) {
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        pendingRPC.put(message.getMessageId(), future);
        channel.writeAndFlush(message);
        return future;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) throws Exception {
        byte messageType = msg.getMessageType();
        if (messageType == ProtocolConstants.TYPE_HEARTBEAT_RESPONSE) {
            System.out.println("Client received heartbeat PONG from server: " + ctx.channel().remoteAddress());
        } else if (messageType == ProtocolConstants.TYPE_RESPONSE) {
            CompletableFuture<RpcResponse> future = pendingRPC.remove(msg.getMessageId());
            if (future != null) {
                future.complete((RpcResponse) msg.getData());
            }
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                System.out.println("Client sending heartbeat...");
                RpcRequest heartbeatRequest = new RpcRequest();
                RpcMessage heartbeatMessage = RpcMessage.builder()
                        .serializerType(ProtocolConstants.SERIALIZER_PROTOSTUFF)
                        .messageType(ProtocolConstants.TYPE_HEARTBEAT_REQUEST)
                        .messageId(MessageIdGenerator.nextId())
                        .data(heartbeatRequest)
                        .build();
                ctx.writeAndFlush(heartbeatMessage);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 当发生异常时，确保所有等待的future都能被异常终止
        pendingRPC.values().forEach(future -> future.completeExceptionally(cause));
        pendingRPC.clear();
        cause.printStackTrace();
        ctx.close();
    }
}