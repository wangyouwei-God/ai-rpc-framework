package com.aicore.rpc.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitBreakerRegistry.
 */
class CircuitBreakerRegistryTest {

    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        // Get a fresh instance for each test
        registry = CircuitBreakerRegistry.getInstance();
    }

    @Test
    @DisplayName("Singleton instance should not be null")
    void testGetInstance() {
        assertNotNull(registry);
        assertSame(registry, CircuitBreakerRegistry.getInstance());
    }

    @Test
    @DisplayName("getOrCreate should create new circuit breaker")
    void testGetOrCreateNew() {
        String key = "test-service-" + System.nanoTime();
        CircuitBreaker cb = registry.getOrCreate(key);

        assertNotNull(cb);
        assertEquals(CircuitBreakerState.CLOSED, cb.getState());
    }

    @Test
    @DisplayName("getOrCreate should return cached instance")
    void testGetOrCreateCached() {
        String key = "cached-service-" + System.nanoTime();
        CircuitBreaker first = registry.getOrCreate(key);
        CircuitBreaker second = registry.getOrCreate(key);

        assertSame(first, second);
    }

    @Test
    @DisplayName("get should return null for unknown key")
    void testGetUnknown() {
        String key = "unknown-service-" + System.nanoTime();
        CircuitBreaker cb = registry.get(key);

        assertNull(cb);
    }

    @Test
    @DisplayName("get should return existing circuit breaker")
    void testGetExisting() {
        String key = "existing-service-" + System.nanoTime();
        CircuitBreaker created = registry.getOrCreate(key);
        CircuitBreaker retrieved = registry.get(key);

        assertSame(created, retrieved);
    }

    @Test
    @DisplayName("Different keys should have different circuit breakers")
    void testDifferentKeys() {
        String key1 = "service-a-" + System.nanoTime();
        String key2 = "service-b-" + System.nanoTime();

        CircuitBreaker cb1 = registry.getOrCreate(key1);
        CircuitBreaker cb2 = registry.getOrCreate(key2);

        assertNotSame(cb1, cb2);
    }
}
