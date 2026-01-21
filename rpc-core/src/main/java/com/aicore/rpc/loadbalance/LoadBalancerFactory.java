package com.aicore.rpc.loadbalance;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 *
 * 负载均衡策略工厂。
 *
 * 职责:
 * 1. 使用 Java SPI (Service Provider Interface) 机制动态加载所有 LoadBalancer 的实现。
 * 2. 提供一个 get 方法，根据名称获取对应的 LoadBalancer 实例。
 * 3. 实现了单例模式和缓存，避免重复加载。
 *
 * 设计说明:
 * 这是实现框架“可插拔”特性的核心。用户可以自行实现 LoadBalancer 接口，
 * 并通过标准的SPI配置文件将其注入到框架中，而无需修改任何框架源码。
 */
public class LoadBalancerFactory {

    private static final Map<String, LoadBalancer> LOAD_BALANCER_CACHE = new HashMap<>();

    static {
        // 使用 ServiceLoader 加载所有实现了 LoadBalancer 接口的类
        ServiceLoader<LoadBalancer> loader = ServiceLoader.load(LoadBalancer.class);
        for (LoadBalancer loadBalancer : loader) {
            // 我们用实现类的简称（去掉 "LoadBalancer" 后缀，转为小写）作为 key
            // 例如：RandomLoadBalancer -> random, AIPredictiveLoadBalancer -> aipredictive
            String key = loadBalancer.getClass().getSimpleName()
                    .replace("LoadBalancer", "")
                    .toLowerCase();
            LOAD_BALANCER_CACHE.put(key, loadBalancer);
            System.out.println("Loaded LoadBalancer implementation: " + key + " -> " + loadBalancer.getClass().getName());
        }
    }

    /**
     * 根据名称获取负载均衡策略实例。
     * @param name 策略名称 (例如: "random", "aipredictive")
     * @return LoadBalancer 实例
     */
    public static LoadBalancer get(String name) {
        if (name == null || name.isEmpty()) {
            // 如果没有指定名称，可以返回一个默认的策略
            return LOAD_BALANCER_CACHE.get("random");
        }
        LoadBalancer loadBalancer = LOAD_BALANCER_CACHE.get(name.toLowerCase());
        if (loadBalancer == null) {
            throw new IllegalArgumentException("No LoadBalancer found for name: " + name);
        }
        return loadBalancer;
    }
}