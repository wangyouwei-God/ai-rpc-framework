package com.aicore.rpc.loadbalance;

import com.aicore.rpc.circuitbreaker.CircuitBreakerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClientMetricsCollector.
 */
class ClientMetricsCollectorTest {

    private ClientMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = ClientMetricsCollector.getInstance();
    }

    @Test
    @DisplayName("Singleton instance should not be null")
    void testGetInstance() {
        assertNotNull(collector);
        assertSame(collector, ClientMetricsCollector.getInstance());
    }

    @Test
    @DisplayName("Should calculate weight 0 for OPEN circuit breaker")
    void testWeightForOpenCircuitBreaker() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.OPEN);

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(0.0, weight);
    }

    @Test
    @DisplayName("Should calculate reduced weight for HALF_OPEN circuit breaker")
    void testWeightForHalfOpenCircuitBreaker() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.HALF_OPEN);
        metrics.setFailureRate(0);
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(0.3, weight, 0.01);
    }

    @Test
    @DisplayName("Should calculate full weight for CLOSED circuit breaker with no failures")
    void testWeightForHealthyEndpoint() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        metrics.setFailureRate(0);
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(1.0, weight);
    }

    @Test
    @DisplayName("Should reduce weight for high failure rate")
    void testWeightReductionForHighFailureRate() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        metrics.setFailureRate(60); // > 50%
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(0.2, weight, 0.01);
    }

    @Test
    @DisplayName("Should reduce weight for medium failure rate")
    void testWeightReductionForMediumFailureRate() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        metrics.setFailureRate(30); // > 20%
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(0.5, weight, 0.01);
    }

    @Test
    @DisplayName("Should reduce weight for high slow call rate")
    void testWeightReductionForHighSlowCallRate() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        metrics.setFailureRate(0);
        metrics.setSlowCallRate(60); // > 50%

        double weight = collector.calculateLocalWeight(metrics);

        assertEquals(0.5, weight, 0.01);
    }

    @Test
    @DisplayName("EndpointMetrics toMap should contain all fields")
    void testEndpointMetricsToMap() {
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setAddress(new java.net.InetSocketAddress("127.0.0.1", 8080));
        metrics.setCircuitBreakerState(CircuitBreakerState.CLOSED);
        metrics.setFailureRate(10);
        metrics.setSlowCallRate(5);
        metrics.setTotalCalls(100);
        metrics.setP50Latency(50);
        metrics.setP99Latency(200);
        metrics.setAvgLatency(75.5);
        metrics.setSampleCount(100);

        java.util.Map<String, Object> map = metrics.toMap();

        assertEquals("127.0.0.1:8080", map.get("address"));
        assertEquals("CLOSED", map.get("circuitBreakerState"));
        assertEquals(10f, map.get("failureRate"));
        assertEquals(5f, map.get("slowCallRate"));
        assertEquals(100, map.get("totalCalls"));
        assertEquals(50L, map.get("p50Latency"));
        assertEquals(200L, map.get("p99Latency"));
        assertEquals(75.5, map.get("avgLatency"));
        assertEquals(100, map.get("sampleCount"));
    }
}
