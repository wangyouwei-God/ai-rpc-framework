package com.aicore.rpc.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务注册与发现的顶级接口。
 * 定义了服务提供者和服务消费者与注册中心交互的基本操作。
 */
public interface Registry {

    /**
     * 将服务实例注册到注册中心。
     *
     * @param serviceName 服务名称
     * @param address     服务实例的地址 (IP:Port)
     */
    void register(String serviceName, InetSocketAddress address);

    /**
     * 从注册中心注销一个服务实例。
     * 这是实现优雅停机的关键一步。
     *
     * @param serviceName 服务名称
     * @param address     待注销的服务实例地址
     */
    void deregister(String serviceName, InetSocketAddress address);

    /**
     * 根据服务名称查询所有可用的服务实例地址列表。
     *
     * @param serviceName 服务名称
     * @return 服务实例地址列表
     */
    List<InetSocketAddress> discover(String serviceName);
}