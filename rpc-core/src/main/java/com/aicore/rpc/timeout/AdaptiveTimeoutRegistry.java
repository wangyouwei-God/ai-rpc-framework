package com.aicore.rpc.timeout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing AdaptiveTimeout instances.
 * Each service endpoint should have its own AdaptiveTimeout.
 */
public class AdaptiveTimeoutRegistry {

    private static final AdaptiveTimeoutRegistry INSTANCE = new AdaptiveTimeoutRegistry();

    private final Map<String, AdaptiveTimeout> timeouts;
    private final AdaptiveTimeoutConfig defaultConfig;

    private AdaptiveTimeoutRegistry() {
        this.timeouts = new ConcurrentHashMap<>();
        this.defaultConfig = AdaptiveTimeoutConfig.defaultConfig();
    }

    public static AdaptiveTimeoutRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Get or create an AdaptiveTimeout for the given name.
     * 
     * @param name unique name (usually service:host:port)
     * @return the AdaptiveTimeout instance
     */
    public AdaptiveTimeout getOrCreate(String name) {
        return timeouts.computeIfAbsent(name, n -> new AdaptiveTimeout(n, defaultConfig));
    }

    /**
     * Get or create an AdaptiveTimeout with custom configuration.
     * 
     * @param name   unique name
     * @param config custom configuration
     * @return the AdaptiveTimeout instance
     */
    public AdaptiveTimeout getOrCreate(String name, AdaptiveTimeoutConfig config) {
        return timeouts.computeIfAbsent(name, n -> new AdaptiveTimeout(n, config));
    }

    /**
     * Get an existing AdaptiveTimeout by name.
     * 
     * @param name the timeout name
     * @return the AdaptiveTimeout or null if not found
     */
    public AdaptiveTimeout get(String name) {
        return timeouts.get(name);
    }

    /**
     * Get all registered AdaptiveTimeouts.
     */
    public Map<String, AdaptiveTimeout> getAll() {
        return new ConcurrentHashMap<>(timeouts);
    }

    /**
     * Clear all AdaptiveTimeouts.
     */
    public void clear() {
        timeouts.clear();
    }
}
