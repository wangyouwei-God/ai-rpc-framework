package com.aicore.rpc.circuitbreaker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing CircuitBreaker instances.
 * Each service endpoint should have its own CircuitBreaker.
 */
public class CircuitBreakerRegistry {

    private static final CircuitBreakerRegistry INSTANCE = new CircuitBreakerRegistry();

    private final Map<String, CircuitBreaker> circuitBreakers;
    private final CircuitBreakerConfig defaultConfig;

    private CircuitBreakerRegistry() {
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.defaultConfig = CircuitBreakerConfig.defaultConfig();
    }

    public static CircuitBreakerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Get or create a CircuitBreaker for the given name.
     * Uses default configuration.
     * 
     * @param name unique name for the circuit breaker (usually service:host:port)
     * @return the CircuitBreaker instance
     */
    public CircuitBreaker getOrCreate(String name) {
        return circuitBreakers.computeIfAbsent(name,
                n -> new CircuitBreaker(n, defaultConfig));
    }

    /**
     * Get or create a CircuitBreaker with custom configuration.
     * 
     * @param name   unique name for the circuit breaker
     * @param config custom configuration
     * @return the CircuitBreaker instance
     */
    public CircuitBreaker getOrCreate(String name, CircuitBreakerConfig config) {
        return circuitBreakers.computeIfAbsent(name,
                n -> new CircuitBreaker(n, config));
    }

    /**
     * Get an existing CircuitBreaker by name.
     * 
     * @param name the circuit breaker name
     * @return the CircuitBreaker or null if not found
     */
    public CircuitBreaker get(String name) {
        return circuitBreakers.get(name);
    }

    /**
     * Remove a CircuitBreaker from the registry.
     * 
     * @param name the circuit breaker name
     */
    public void remove(String name) {
        circuitBreakers.remove(name);
    }

    /**
     * Get all registered CircuitBreakers.
     */
    public Map<String, CircuitBreaker> getAll() {
        return new ConcurrentHashMap<>(circuitBreakers);
    }

    /**
     * Clear all CircuitBreakers.
     */
    public void clear() {
        circuitBreakers.clear();
    }
}
