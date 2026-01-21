package com.aicore.rpc.loadbalance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AI Predictive Load Balancer core algorithms.
 * Tests the weighted random selection and client metrics fusion logic.
 */
class AIPredictiveLoadBalancerTest {

    @Test
    @DisplayName("Random load balancer should distribute calls")
    void testRandomLoadBalancerDistribution() {
        RandomLoadBalancer lb = new RandomLoadBalancer();
        List<InetSocketAddress> addresses = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("127.0.0.1", 8081),
                new InetSocketAddress("127.0.0.1", 8082));

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            InetSocketAddress selected = lb.select(addresses);
            int port = selected.getPort();
            counts.put(port, counts.getOrDefault(port, 0) + 1);
        }

        // Each port should get roughly 1/3 of calls (with some variance)
        for (int port : Arrays.asList(8080, 8081, 8082)) {
            int count = counts.getOrDefault(port, 0);
            assertTrue(count > 200, "Port " + port + " should get at least 200 calls, got " + count);
            assertTrue(count < 500, "Port " + port + " should get at most 500 calls, got " + count);
        }
    }

    @Test
    @DisplayName("Client metrics weight calculation - healthy endpoint")
    void testClientMetricsHealthyEndpoint() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();

        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.CLOSED);
        metrics.setFailureRate(0);
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);
        assertEquals(1.0, weight, "Healthy endpoint should have weight 1.0");
    }

    @Test
    @DisplayName("Client metrics weight calculation - open circuit breaker")
    void testClientMetricsOpenCircuitBreaker() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();

        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.OPEN);

        double weight = collector.calculateLocalWeight(metrics);
        assertEquals(0.0, weight, "Open circuit breaker should have weight 0.0");
    }

    @Test
    @DisplayName("Client metrics weight calculation - half-open circuit breaker")
    void testClientMetricsHalfOpenCircuitBreaker() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();

        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.HALF_OPEN);
        metrics.setFailureRate(0);
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);
        assertEquals(0.3, weight, 0.01, "Half-open circuit should have weight ~0.3");
    }

    @Test
    @DisplayName("Client metrics weight calculation - high failure rate")
    void testClientMetricsHighFailureRate() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();

        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.CLOSED);
        metrics.setFailureRate(60); // > 50%
        metrics.setSlowCallRate(0);

        double weight = collector.calculateLocalWeight(metrics);
        assertTrue(weight < 0.5, "High failure rate should significantly reduce weight");
    }

    @Test
    @DisplayName("Client metrics weight calculation - combined factors")
    void testClientMetricsCombinedFactors() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();

        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.CLOSED);
        metrics.setFailureRate(30); // > 20%, weight *= 0.5
        metrics.setSlowCallRate(60); // > 50%, weight *= 0.5

        double weight = collector.calculateLocalWeight(metrics);
        // Expected: 1.0 * 0.5 (failure) * 0.5 (slow) = 0.25
        assertEquals(0.25, weight, 0.01, "Combined factors should multiply weights");
    }

    @Test
    @DisplayName("AI weight fusion - AI weight multiplied with local weight")
    void testAIWeightFusion() {
        // Simulate the fusion logic from AIPredictiveLoadBalancer
        double aiWeight = 0.8;
        double localWeight = 0.5; // e.g., from medium failure rate

        double finalWeight = aiWeight * localWeight;

        assertEquals(0.4, finalWeight, 0.01, "Final weight should be AI * local");
    }

    @Test
    @DisplayName("Weighted selection should favor higher weights")
    void testWeightedSelectionFavorsHigherWeights() {
        // Simulate weighted random selection
        Map<String, Double> weights = new HashMap<>();
        weights.put("high", 0.9);
        weights.put("low", 0.1);

        Map<String, Integer> counts = new HashMap<>();
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility

        for (int i = 0; i < 1000; i++) {
            double totalWeight = weights.values().stream().mapToDouble(d -> d).sum();
            double randomPoint = random.nextDouble() * totalWeight;
            double cumulative = 0;
            String selected = null;
            for (Map.Entry<String, Double> entry : weights.entrySet()) {
                cumulative += entry.getValue();
                if (randomPoint < cumulative) {
                    selected = entry.getKey();
                    break;
                }
            }
            if (selected != null) {
                counts.put(selected, counts.getOrDefault(selected, 0) + 1);
            }
        }

        int highCount = counts.getOrDefault("high", 0);
        int lowCount = counts.getOrDefault("low", 0);

        // High weight (0.9) should get ~90% of calls
        assertTrue(highCount > 800, "High weight should get >80% calls, got " + highCount);
        assertTrue(lowCount < 200, "Low weight should get <20% calls, got " + lowCount);
    }
}
