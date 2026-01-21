package com.aicore.rpc.client;

import com.aicore.rpc.codec.ProtostuffSerializer;
import com.aicore.rpc.codec.RpcDecoder;
import com.aicore.rpc.codec.RpcEncoder;
import com.aicore.rpc.codec.Serializer;
import com.aicore.rpc.message.MessageIdGenerator;
import com.aicore.rpc.message.ProtocolConstants;
import com.aicore.rpc.message.RpcMessage;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.model.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RpcClient {
    private final String host;
    private final int port;
    private Channel channel;

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();

        final SslContext sslCtx = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        Serializer serializer = new ProtostuffSerializer();
                        ch.pipeline()
                                .addLast(sslCtx.newHandler(ch.alloc(), host, port))
                                .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new RpcClientHandler()); // 每个连接都有自己的Handler实例
                    }
                })
                .option(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture f = b.connect(host, port).sync();
        this.channel = f.channel();
    }

    public CompletableFuture<RpcResponse> send(RpcRequest request) throws Exception {
        if (channel == null || !channel.isActive()) {
            connect();
        }
        RpcClientHandler handler = channel.pipeline().get(RpcClientHandler.class);
        RpcMessage message = RpcMessage.builder()
                .serializerType(ProtocolConstants.SERIALIZER_PROTOSTUFF)
                .messageType(ProtocolConstants.TYPE_REQUEST)
                .messageId(MessageIdGenerator.nextId())
                .data(request).build();
        return handler.sendRequest(message, channel);
    }
}