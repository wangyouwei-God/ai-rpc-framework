package com.aicore.rpc.timeout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdaptiveTimeoutRegistry.
 */
class AdaptiveTimeoutRegistryTest {

    private AdaptiveTimeoutRegistry registry;

    @BeforeEach
    void setUp() {
        registry = AdaptiveTimeoutRegistry.getInstance();
    }

    @Test
    @DisplayName("Singleton instance should not be null")
    void testGetInstance() {
        assertNotNull(registry);
        assertSame(registry, AdaptiveTimeoutRegistry.getInstance());
    }

    @Test
    @DisplayName("getOrCreate should create new adaptive timeout")
    void testGetOrCreateNew() {
        String key = "timeout-service-" + System.nanoTime();
        AdaptiveTimeout at = registry.getOrCreate(key);

        assertNotNull(at);
    }

    @Test
    @DisplayName("getOrCreate should return cached instance")
    void testGetOrCreateCached() {
        String key = "cached-timeout-" + System.nanoTime();
        AdaptiveTimeout first = registry.getOrCreate(key);
        AdaptiveTimeout second = registry.getOrCreate(key);

        assertSame(first, second);
    }

    @Test
    @DisplayName("get should return null for unknown key")
    void testGetUnknown() {
        String key = "unknown-timeout-" + System.nanoTime();
        AdaptiveTimeout at = registry.get(key);

        assertNull(at);
    }

    @Test
    @DisplayName("get should return existing adaptive timeout")
    void testGetExisting() {
        String key = "existing-timeout-" + System.nanoTime();
        AdaptiveTimeout created = registry.getOrCreate(key);
        AdaptiveTimeout retrieved = registry.get(key);

        assertSame(created, retrieved);
    }

    @Test
    @DisplayName("Different keys should have different adaptive timeouts")
    void testDifferentKeys() {
        String key1 = "timeout-a-" + System.nanoTime();
        String key2 = "timeout-b-" + System.nanoTime();

        AdaptiveTimeout at1 = registry.getOrCreate(key1);
        AdaptiveTimeout at2 = registry.getOrCreate(key2);

        assertNotSame(at1, at2);
    }
}
