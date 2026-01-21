package com.aicore.rpc.loadbalance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoadBalancerFactory.
 */
class LoadBalancerFactoryTest {

    @Test
    @DisplayName("Should return random load balancer for empty name")
    void testGetDefaultLoadBalancer() {
        LoadBalancer lb = LoadBalancerFactory.get("");
        assertNotNull(lb);
        assertTrue(lb instanceof RandomLoadBalancer);
    }

    @Test
    @DisplayName("Should return random load balancer for null name")
    void testGetNullReturnsDefault() {
        LoadBalancer lb = LoadBalancerFactory.get(null);
        assertNotNull(lb);
        assertTrue(lb instanceof RandomLoadBalancer);
    }

    @Test
    @DisplayName("Should return random load balancer by name")
    void testGetRandomLoadBalancer() {
        LoadBalancer lb = LoadBalancerFactory.get("random");
        assertNotNull(lb);
        assertTrue(lb instanceof RandomLoadBalancer);
    }

    @Test
    @DisplayName("Should return AI predictive load balancer by name")
    void testGetAIPredictiveLoadBalancer() {
        LoadBalancer lb = LoadBalancerFactory.get("aipredictive");
        assertNotNull(lb);
        assertTrue(lb instanceof AIPredictiveLoadBalancer);
    }

    @Test
    @DisplayName("Should be case insensitive")
    void testCaseInsensitive() {
        LoadBalancer lb1 = LoadBalancerFactory.get("RANDOM");
        LoadBalancer lb2 = LoadBalancerFactory.get("Random");
        LoadBalancer lb3 = LoadBalancerFactory.get("random");

        assertSame(lb1, lb2);
        assertSame(lb2, lb3);
    }

    @Test
    @DisplayName("Should throw exception for unknown load balancer")
    void testUnknownLoadBalancer() {
        assertThrows(IllegalArgumentException.class, () -> {
            LoadBalancerFactory.get("nonexistent");
        });
    }
}
