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

            // 等待HTTP连接就绪（这是快速检查）
            boolean isConnected = false;
            long startTime = System.currentTimeMillis();
            long timeout = TimeUnit.SECONDS.toMillis(30);

            while (!isConnected && (System.currentTimeMillis() - startTime < timeout)) {
                String serverStatus = namingService.getServerStatus();
                if ("UP".equals(serverStatus)) {
                    isConnected = true;
                    System.out.println("Nacos HTTP connection established.");
                } else {
                    System.out.println("Waiting for Nacos connection... status: " + serverStatus);
                    Thread.sleep(1000);
                }
            }
            if (!isConnected) {
                throw new RuntimeException("Failed to connect to Nacos within " + timeout / 1000 + " seconds.");
            }

            // 注意：gRPC客户端是异步初始化的，可能需要更长时间
            // 服务注册时会通过重试机制等待gRPC就绪
            System.out
                    .println("NacosRegistry initialized. gRPC client will be ready when first registration succeeds.");

        } catch (NacosException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to initialize NacosRegistry", e);
        }
    }

    @Override
    public void register(String serviceName, InetSocketAddress address) {
        // 使用指数退避重试，等待gRPC客户端就绪
        int maxRetries = 10;
        long baseDelayMs = 1000;
        long maxDelayMs = 10000;
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                namingService.registerInstance(serviceName, address.getHostName(), address.getPort());
                System.out.println("Service registered successfully: " + serviceName + " at " + address);
                return;
            } catch (NacosException e) {
                lastException = e;
                // 计算指数退避延迟
                long delay = Math.min(baseDelayMs * (1L << attempt), maxDelayMs);
                System.out.println("Register attempt " + (attempt + 1) + "/" + maxRetries + " failed: " +
                        e.getErrMsg() + ". Retrying in " + delay + "ms...");
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during registration retry", ie);
                }
            }
        }
        throw new RuntimeException("Failed to register service after " + maxRetries + " attempts. " +
                "Last error: " + (lastException != null ? lastException.getMessage() : "unknown"), lastException);
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