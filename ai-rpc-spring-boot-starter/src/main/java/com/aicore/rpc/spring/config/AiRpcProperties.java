package com.aicore.rpc.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI-RPC framework.
 * Properties are bound from application.yml or application.properties.
 *
 * <p>
 * Example configuration:
 * </p>
 * 
 * <pre>
 * ai-rpc:
 *   registry:
 *     address: 127.0.0.1:8848
 *   server:
 *     port: 9000
 *   client:
 *     timeout: 10000
 *   loadbalancer:
 *     type: ai
 *     ai-service-url: http://localhost:8000/predict
 * </pre>
 */
@ConfigurationProperties(prefix = "ai-rpc")
public class AiRpcProperties {

    /**
     * Registry configuration.
     */
    private Registry registry = new Registry();

    /**
     * Server configuration.
     */
    private Server server = new Server();

    /**
     * Client configuration.
     */
    private Client client = new Client();

    /**
     * Load balancer configuration.
     */
    private LoadBalancer loadbalancer = new LoadBalancer();

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LoadBalancer getLoadbalancer() {
        return loadbalancer;
    }

    public void setLoadbalancer(LoadBalancer loadbalancer) {
        this.loadbalancer = loadbalancer;
    }

    /**
     * Registry configuration.
     */
    public static class Registry {

        /**
         * Registry type. Currently supports: nacos
         */
        private String type = "nacos";

        /**
         * Registry server address.
         */
        private String address = "127.0.0.1:8848";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }
    }

    /**
     * Server configuration.
     */
    public static class Server {

        /**
         * RPC server port.
         */
        private int port = 9000;

        /**
         * IO thread count.
         */
        private int ioThreads = 0;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getIoThreads() {
            return ioThreads;
        }

        public void setIoThreads(int ioThreads) {
            this.ioThreads = ioThreads;
        }
    }

    /**
     * Client configuration.
     */
    public static class Client {

        /**
         * Request timeout in milliseconds.
         */
        private int timeout = 10000;

        /**
         * Number of retry attempts.
         */
        private int retries = 2;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }
    }

    /**
     * Load balancer configuration.
     */
    public static class LoadBalancer {

        /**
         * Load balancer type. Options: random, ai
         */
        private String type = "ai";

        /**
         * AI prediction service URL.
         */
        private String aiServiceUrl = "http://localhost:8000/predict";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAiServiceUrl() {
            return aiServiceUrl;
        }

        public void setAiServiceUrl(String aiServiceUrl) {
            this.aiServiceUrl = aiServiceUrl;
        }
    }
}
