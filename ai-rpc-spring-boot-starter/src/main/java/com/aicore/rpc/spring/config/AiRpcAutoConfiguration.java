package com.aicore.rpc.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aicore.rpc.loadbalance.AIPredictiveLoadBalancer;
import com.aicore.rpc.loadbalance.LoadBalancer;
import com.aicore.rpc.loadbalance.RandomLoadBalancer;
import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.registry.nacos.NacosRegistry;
import com.aicore.rpc.spring.processor.AiRpcReferenceProcessor;
import com.aicore.rpc.spring.processor.AiRpcServiceProcessor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Auto-configuration for AI-RPC framework.
 * This class configures all necessary beans for RPC client and server.
 */
@Configuration
@EnableConfigurationProperties(AiRpcProperties.class)
public class AiRpcAutoConfiguration {

    @Autowired
    private AiRpcProperties properties;

    /**
     * Create registry bean based on configuration.
     */
    @Bean
    @ConditionalOnMissingBean(Registry.class)
    public Registry registry() {
        String registryType = properties.getRegistry().getType();
        String registryAddress = properties.getRegistry().getAddress();

        if ("nacos".equalsIgnoreCase(registryType)) {
            return new NacosRegistry(registryAddress);
        }

        // Default to Nacos
        return new NacosRegistry(registryAddress);
    }

    /**
     * Create load balancer bean based on configuration.
     */
    @Bean
    @ConditionalOnMissingBean(LoadBalancer.class)
    public LoadBalancer loadBalancer() {
        String type = properties.getLoadbalancer().getType();

        if ("ai".equalsIgnoreCase(type)) {
            return new AIPredictiveLoadBalancer();
        } else if ("random".equalsIgnoreCase(type)) {
            return new RandomLoadBalancer();
        }

        // Default to AI load balancer
        return new AIPredictiveLoadBalancer();
    }

    /**
     * Create meter registry for metrics collection.
     */
    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * Create processor for @AiRpcReference annotations.
     */
    @Bean
    public AiRpcReferenceProcessor aiRpcReferenceProcessor(Registry registry,
            LoadBalancer loadBalancer,
            MeterRegistry meterRegistry) {
        return new AiRpcReferenceProcessor(registry, loadBalancer, meterRegistry, properties);
    }

    /**
     * Create processor for @AiRpcService annotations.
     */
    @Bean
    public AiRpcServiceProcessor aiRpcServiceProcessor(Registry registry) {
        return new AiRpcServiceProcessor(registry, properties);
    }
}
