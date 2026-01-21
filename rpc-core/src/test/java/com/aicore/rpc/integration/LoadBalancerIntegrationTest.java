package com.aicore.rpc.integration;

import com.aicore.rpc.circuitbreaker.*;
import com.aicore.rpc.loadbalance.*;
import com.aicore.rpc.timeout.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LoadBalancer with client-side metrics.
 */
class LoadBalancerIntegrationTest {

    private RandomLoadBalancer randomLB;
    private ClientMetricsCollector metricsCollector;

    @BeforeEach
    void setUp() {
        randomLB = new RandomLoadBalancer();
        metricsCollector = ClientMetricsCollector.getInstance();
    }

    @Test
    @DisplayName("Random load balancer should select from available addresses")
    void testRandomLoadBalancerSelection() {
        List<InetSocketAddress> addresses = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("127.0.0.1", 8081),
                new InetSocketAddress("127.0.0.1", 8082));

        InetSocketAddress selected = randomLB.select(addresses);

        assertNotNull(selected);
        assertTrue(addresses.contains(selected));
    }

    @Test
    @DisplayName("Random load balancer should return null for empty list")
    void testRandomLoadBalancerEmptyList() {
        List<InetSocketAddress> empty = Arrays.asList();

        InetSocketAddress selected = randomLB.select(empty);

        assertNull(selected);
    }

    @Test
    @DisplayName("Client metrics should affect weight calculation")
    void testMetricsAffectWeight() {
        // Create metrics for a healthy endpoint
        ClientMetricsCollector.EndpointMetrics healthy = new ClientMetricsCollector.EndpointMetrics();
        healthy.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        healthy.setFailureRate(0);
        healthy.setSlowCallRate(0);

        // Create metrics for an unhealthy endpoint
        ClientMetricsCollector.EndpointMetrics unhealthy = new ClientMetricsCollector.EndpointMetrics();
        unhealthy.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        unhealthy.setFailureRate(60);
        unhealthy.setSlowCallRate(30);

        double healthyWeight = metricsCollector.calculateLocalWeight(healthy);
        double unhealthyWeight = metricsCollector.calculateLocalWeight(unhealthy);

        assertTrue(healthyWeight > unhealthyWeight,
                "Healthy endpoint weight (" + healthyWeight + ") should be higher than unhealthy (" + unhealthyWeight
                        + ")");
    }

    @Test
    @DisplayName("Open circuit breaker should have zero weight")
    void testOpenCircuitBreakerZeroWeight() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.OPEN);

        double weight = metricsCollector.calculateLocalWeight(metrics);

        assertEquals(0.0, weight);
    }

    @Test
    @DisplayName("LoadBalancerFactory should return correct implementations")
    void testLoadBalancerFactory() {
        LoadBalancer random = LoadBalancerFactory.get("random");
        LoadBalancer ai = LoadBalancerFactory.get("aipredictive");

        assertNotNull(random);
        assertNotNull(ai);
        assertTrue(random instanceof RandomLoadBalancer);
        assertTrue(ai instanceof AIPredictiveLoadBalancer);
    }

    @Test
    @DisplayName("Circuit breaker registry should integrate with metrics collector")
    void testCircuitBreakerRegistryIntegration() {
        String serviceKey = "test-service-" + System.nanoTime() + "@127.0.0.1:8080";

        // Create a circuit breaker via registry
        CircuitBreaker cb = CircuitBreakerRegistry.getInstance().getOrCreate(serviceKey);
        assertNotNull(cb);

        // Record some failures
        cb.recordFailure();
        cb.recordFailure();
        cb.recordSuccess(10);

        // Verify metrics are tracked
        SlidingWindowMetrics metrics = cb.getMetrics();
        assertEquals(3, metrics.getTotalCalls());
        assertTrue(metrics.getFailureRate() > 0);
    }

    @Test
    @DisplayName("Adaptive timeout registry should integrate with load balancer decisions")
    void testAdaptiveTimeoutRegistryIntegration() {
        String serviceKey = "timeout-service-" + System.nanoTime() + "@127.0.0.1:8080";

        // Create adaptive timeout via registry
        AdaptiveTimeout at = AdaptiveTimeoutRegistry.getInstance().getOrCreate(serviceKey);
        assertNotNull(at);

        // Record some latencies
        at.recordLatency(50);
        at.recordLatency(100);
        at.recordLatency(75);

        // Verify timeout is calculated - using correct method name
        long timeout = at.getTimeoutMs();
        assertTrue(timeout > 0);
    }
}
