package com.aicore.rpc.registry.nacos;

import com.aicore.rpc.registry.Registry;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Nacos 的服务注册与发现实现。
 */
public class NacosRegistry implements Registry {

    private final NamingService namingService;

    public NacosRegistry(String nacosAddress) {
        try {
            this.namingService = NamingFactory.createNamingService(nacosAddress);

            // 阻塞等待，直到与Nacos Server的连接建立成功或超时
            boolean isConnected = false;
            long startTime = System.currentTimeMillis();
            long timeout = TimeUnit.SECONDS.toMillis(15);

            while (!isConnected && (System.currentTimeMillis() - startTime < timeout)) {
                String serverStatus = namingService.getServerStatus();
                if ("UP".equals(serverStatus)) {
                    isConnected = true;
                    System.out.println("Successfully connected to Nacos server.");
                } else {
                    System.out.println("Waiting for Nacos connection... current status: " + serverStatus);
                    Thread.sleep(1000);
                }
            }
            if (!isConnected) {
                throw new RuntimeException("Failed to connect to Nacos server within " + timeout / 1000 + " seconds.");
            }
        } catch (NacosException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to initialize NacosRegistry", e);
        }
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) {
        try {
            namingService.registerInstance(serviceName, address.getHostName(), address.getPort());
        } catch (NacosException e) {
            throw new RuntimeException("Failed to register service to Nacos", e);
        }
    }

    /**
     * 实现服务注销逻辑。
     * 在优雅停机时被调用。
     */
    @Override
    public void deregister(String serviceName, InetSocketAddress address) {
        try {
            namingService.deregisterInstance(serviceName, address.getHostName(), address.getPort());
            System.out.println("Service deregistered successfully: " + serviceName + " at " + address);
        } catch (NacosException e) {
            // 在关闭流程中，即使注销失败也只记录错误日志，不应抛出异常中断关闭。
            System.err.println("Failed to deregister service '" + serviceName + "' from Nacos: " + e.getMessage());
        }
    }


    @Override
    public List<InetSocketAddress> discover(String serviceName) {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, true);
            return instances.stream()
                    .map(instance -> new InetSocketAddress(instance.getIp(), instance.getPort()))
                    .collect(Collectors.toList());
        } catch (NacosException e) {
            throw new RuntimeException("Failed to discover service from Nacos", e);
        }
    }
}