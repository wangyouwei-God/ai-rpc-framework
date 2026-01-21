package com.aicore.rpc.server;

import com.aicore.rpc.codec.ProtostuffSerializer;
import com.aicore.rpc.codec.RpcDecoder;
import com.aicore.rpc.codec.RpcEncoder;
import com.aicore.rpc.codec.Serializer;
import com.aicore.rpc.model.RpcRequest;
import com.aicore.rpc.registry.Registry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RPC 服务器实现。
 * 封装了 Netty ServerBootstrap 的启动、服务注册和优雅停机逻辑。
 */
public class RpcServer {
    private final int port;
    private final Registry registry;
    private final Map<String, Object> serviceRegistry = new HashMap<>();

    // 将 Netty 的核心组件提升为成员变量，以便在 shutdown 方法中访问
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public RpcServer(int port, Registry registry) {
        this.port = port;
        this.registry = registry;
    }

    public void registerService(Object service) {
        String serviceName = service.getClass().getInterfaces()[0].getName();
        serviceRegistry.put(serviceName, service);
        System.out.println("Registered service implementation: " + service.getClass().getName() + " for interface: " + serviceName);

        // RPC Server 自身的IP地址，生产环境应动态获取
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
        registry.register(serviceName, address);
    }

    /**
     * 启动 Netty 服务器。
     * 此方法现在是非阻塞的，会立即返回。
     *
     * @throws Exception
     */
    public void start() throws Exception {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();

        InputStream certIn = RpcServer.class.getClassLoader().getResourceAsStream("certificate.crt");
        InputStream keyIn = RpcServer.class.getClassLoader().getResourceAsStream("private.key");
        if (certIn == null || keyIn == null) {
            throw new IllegalStateException("SSL certificate or private key not found in resources.");
        }
        final SslContext sslCtx = SslContextBuilder.forServer(certIn, keyIn).build();

        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        Serializer serializer = new ProtostuffSerializer();
                        ch.pipeline()
                                .addLast(sslCtx.newHandler(ch.alloc()))
                                .addLast(new RpcDecoder())
                                .addLast(new RpcEncoder())
                                .addLast(new RpcServerHandler(serviceRegistry));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // 绑定端口并保存 ChannelFuture，以便后续可以获取 Channel
        this.channelFuture = b.bind(port).sync();
        System.out.println("Server started and listening on port: " + port);
    }

    /**
     * 实现优雅停机逻辑。
     * 在 JVM Shutdown Hook 中被调用。
     */
    public void shutdown() {
        System.out.println("Starting graceful shutdown of RPC server...");

        try {
            // 1. 从注册中心注销所有服务，停止新流量进入
            for (String serviceName : serviceRegistry.keySet()) {
                registry.deregister(serviceName, new InetSocketAddress("127.0.0.1", port));
            }

            // 2. 关闭服务器 Channel，不再接受新的 TCP 连接
            if (channelFuture != null) {
                channelFuture.channel().close().syncUninterruptibly();
            }
        } finally {
            // 3. 优雅地关闭 Netty 线程池，并设置超时时间
            // shutdownGracefully 会等待正在执行的任务完成
            if (bossGroup != null) {
                bossGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully(1, 5, TimeUnit.SECONDS);
            }
        }
        System.out.println("RPC server gracefully shut down.");
    }

    /**
     * 提供一个 getter 方法，供外部（ProviderMain）获取 ChannelFuture。
     * 主要用于让主线程在 `closeFuture()` 上阻塞等待。
     *
     * @return ChannelFuture
     */
    public ChannelFuture getChannelFuture() {
        return this.channelFuture;
    }
}