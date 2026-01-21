package com.aicore.rpc.spring.processor;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import com.aicore.rpc.loadbalance.LoadBalancer;
import com.aicore.rpc.proxy.RpcProxy;
import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.spring.annotation.AiRpcReference;
import com.aicore.rpc.spring.config.AiRpcProperties;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Bean post processor that handles @AiRpcReference annotations.
 * For each field annotated with @AiRpcReference, this processor creates
 * an RPC proxy and injects it into the field.
 */
public class AiRpcReferenceProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AiRpcReferenceProcessor.class);

    private final Registry registry;
    private final LoadBalancer loadBalancer;
    private final MeterRegistry meterRegistry;
    private final AiRpcProperties properties;

    public AiRpcReferenceProcessor(Registry registry,
            LoadBalancer loadBalancer,
            MeterRegistry meterRegistry,
            AiRpcProperties properties) {
        this.registry = registry;
        this.loadBalancer = loadBalancer;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();

        // Scan all fields for @AiRpcReference
        for (Field field : clazz.getDeclaredFields()) {
            AiRpcReference reference = field.getAnnotation(AiRpcReference.class);
            if (reference != null) {
                injectRpcProxy(bean, field, reference);
            }
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Create RPC proxy and inject into the annotated field.
     */
    private void injectRpcProxy(Object bean, Field field, AiRpcReference reference) {
        Class<?> interfaceClass = field.getType();

        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException(
                    "@AiRpcReference can only be used on interface fields: " + field.getName());
        }

        logger.info("Creating RPC proxy for interface: {}", interfaceClass.getName());

        // Create proxy
        Object proxy = RpcProxy.create(interfaceClass, registry, loadBalancer, meterRegistry);

        // Inject proxy into field
        field.setAccessible(true);
        try {
            field.set(bean, proxy);
            logger.info("Injected RPC proxy for field: {}.{}",
                    bean.getClass().getSimpleName(), field.getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject RPC proxy for field: " + field.getName(), e);
        }
    }
}
