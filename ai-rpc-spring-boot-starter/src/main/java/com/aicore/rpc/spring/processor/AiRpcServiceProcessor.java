package com.aicore.rpc.spring.processor;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.server.RpcServer;
import com.aicore.rpc.spring.annotation.AiRpcService;
import com.aicore.rpc.spring.config.AiRpcProperties;

/**
 * Processor that handles @AiRpcService annotations.
 * Scans for beans annotated with @AiRpcService, starts RPC server,
 * and registers services with the registry.
 */
public class AiRpcServiceProcessor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(AiRpcServiceProcessor.class);

    private final Registry registry;
    private final AiRpcProperties properties;
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;
    private RpcServer rpcServer;

    public AiRpcServiceProcessor(Registry registry, AiRpcProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Initialize RPC server and register services after Spring context is ready.
     */
    @PostConstruct
    public void init() throws Exception {
        // Scan for @AiRpcService beans
        Map<String, Object> serviceBeans = applicationContext.getBeansWithAnnotation(AiRpcService.class);

        if (serviceBeans.isEmpty()) {
            logger.info("No @AiRpcService beans found, skipping RPC server startup");
            return;
        }

        // Register services
        for (Object serviceBean : serviceBeans.values()) {
            registerService(serviceBean);
        }

        // Start RPC server
        startRpcServer();
    }

    /**
     * Register a service bean.
     */
    private void registerService(Object serviceBean) {
        Class<?> clazz = serviceBean.getClass();
        AiRpcService annotation = clazz.getAnnotation(AiRpcService.class);

        // Determine service interface
        Class<?> interfaceClass = annotation.interfaceClass();
        if (interfaceClass == void.class) {
            // Use the first interface implemented by the class
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length == 0) {
                throw new IllegalStateException(
                        "@AiRpcService class must implement an interface: " + clazz.getName());
            }
            interfaceClass = interfaces[0];
        }

        String serviceName = interfaceClass.getName();
        serviceMap.put(serviceName, serviceBean);
        logger.info("Registered RPC service: {} -> {}", serviceName, clazz.getName());
    }

    /**
     * Start the RPC server and register with registry.
     */
    private void startRpcServer() throws Exception {
        int port = properties.getServer().getPort();
        String hostAddress = InetAddress.getLocalHost().getHostAddress();

        // Create RPC server with correct constructor signature
        rpcServer = new RpcServer(port, registry);

        // Register each service with the RPC server
        for (Object serviceBean : serviceMap.values()) {
            rpcServer.registerService(serviceBean);
        }

        // Start server in a new thread
        new Thread(() -> {
            try {
                rpcServer.start();
            } catch (Exception e) {
                logger.error("RPC server failed to start", e);
            }
        }, "rpc-server").start();

        logger.info("RPC server starting on {}:{}", hostAddress, port);
    }

    /**
     * Shutdown RPC server on application exit.
     */
    @PreDestroy
    public void destroy() {
        if (rpcServer != null) {
            logger.info("Shutting down RPC server...");
            rpcServer.shutdown();
        }
    }
}
