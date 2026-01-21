package com.aicore.rpc.client.pool;

import com.aicore.rpc.client.RpcClientHandler;
import com.aicore.rpc.codec.ProtostuffSerializer;
import com.aicore.rpc.codec.RpcDecoder;
import com.aicore.rpc.codec.RpcEncoder;
import com.aicore.rpc.codec.Serializer;
import com.aicore.rpc.model.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author YourName
 * @date 2023-10-28
 *
 * 连接池管理器
 *
 * 职责:
 * 1. 维护一个从 "目标地址"到"连接池"的映射。
 * 2. 当外部请求一个到目标地址的连接时，提供一个可用的 Channel。
 * 3. 如果连接池不存在，则为目标地址创建一个新的连接池。
 * 4. 封装 Netty 的 FixedChannelPool，使其易于使用。
 *
 * 设计说明:
 * 采用单例模式，确保整个RPC客户端共享同一个连接池管理器。
 * 使用 ConcurrentHashMap 存储连接池，保证线程安全。
 * 连接池的配置（如大小、超时时间）可以后续通过配置中心进行动态调整。
 */
public class ChannelPoolManager {

    private static volatile ChannelPoolManager instance;
    private final Map<InetSocketAddress, ChannelPool> channelPools = new ConcurrentHashMap<>();
    private final Bootstrap bootstrap;
    private final NioEventLoopGroup group;

    private ChannelPoolManager() {
        this.group = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    public static ChannelPoolManager getInstance() {
        if (instance == null) {
            synchronized (ChannelPoolManager.class) {
                if (instance == null) {
                    instance = new ChannelPoolManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取或创建到指定地址的连接池
     * @param address 目标服务器地址
     * @return 连接池实例
     */
    public ChannelPool getOrCreatePool(InetSocketAddress address) {
        // computeIfAbsent 是一个原子操作，保证了连接池的单例创建
        return channelPools.computeIfAbsent(address, this::createPool);
    }

    /**
     * 为指定地址创建一个新的连接池
     * @param address 目标服务器地址
     * @return 新创建的连接池
     */
    private ChannelPool createPool(InetSocketAddress address) {
        // 连接池的配置
        int maxConnections = 10; // 每个地址的最大连接数

        // AbstractChannelPoolHandler 是一个模板类，我们需要实现它的 channelCreated 和 channelAcquired 方法
        AbstractChannelPoolHandler poolHandler = new AbstractChannelPoolHandler() {
            /**
             * 当连接池创建一个新的 Channel 时，此方法会被调用
             * 职责: 初始化 Channel，为其 Pipeline 添加所有必要的 Handler
             */
            @Override
            public void channelCreated(Channel ch) throws Exception {
                System.out.println("ChannelPool: Created a new channel for " + address);
                // 配置客户端SSL
                final SslContext sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

                Serializer serializer = new ProtostuffSerializer();
                ch.pipeline()
                        .addLast(sslCtx.newHandler(ch.alloc(), address.getHostName(), address.getPort()))
                        .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                        .addLast(new RpcDecoder()) // 注意：解码器不再需要参数
                        .addLast(new RpcEncoder()) // 编码器也不再需要参数
                        .addLast(new RpcClientHandler());
            }

            /**
             * 当从池中获取 Channel 成功时，此方法会被调用
             * 可以用于健康检查等
             */
            @Override
            public void channelAcquired(Channel ch) throws Exception {
                System.out.println("ChannelPool: Acquired channel " + ch.id() + " for " + address);
            }

            /**
             * 当 Channel 被释放回池中时，此方法会被调用
             */
            @Override
            public void channelReleased(Channel ch) throws Exception {
                System.out.println("ChannelPool: Released channel " + ch.id() + " for " + address);
            }
        };

        // 创建一个固定大小的连接池
        return new FixedChannelPool(bootstrap.remoteAddress(address), poolHandler, maxConnections);
    }

    public void shutdown() {
        channelPools.values().forEach(ChannelPool::close);
        group.shutdownGracefully();
    }
}