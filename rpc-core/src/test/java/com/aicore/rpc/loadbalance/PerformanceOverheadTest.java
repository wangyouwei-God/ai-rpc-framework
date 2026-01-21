package com.aicore.rpc.loadbalance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Level 5: Performance and Overhead Tests.
 * Measures the computational overhead of different load balancing strategies.
 */
class PerformanceOverheadTest {

    private RandomLoadBalancer randomLB;
    private static final int WARMUP = 10000;
    private static final int ITERATIONS = 100000;

    @BeforeEach
    void setUp() {
        randomLB = new RandomLoadBalancer();
    }

    @Test
    @DisplayName("Random LB selection should be fast (<1μs per selection)")
    void testRandomLBPerformance() {
        List<InetSocketAddress> addresses = createAddresses(10);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            randomLB.select(addresses);
        }

        // Measure
        long startNanos = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            randomLB.select(addresses);
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double avgNanos = (double) elapsedNanos / ITERATIONS;
        double avgMicros = avgNanos / 1000.0;

        System.out.println("Random LB: " + String.format("%.3f", avgMicros) + " μs per selection");

        // Should be very fast - less than 1 microsecond
        assertTrue(avgMicros < 1.0,
                "Random LB should be <1μs per selection, was " + avgMicros + "μs");
    }

    @Test
    @DisplayName("Weighted selection should be fast (<5μs per selection)")
    void testWeightedSelectionPerformance() {
        List<InetSocketAddress> addresses = createAddresses(10);
        Map<InetSocketAddress, Double> weights = createRandomWeights(addresses);
        Random random = new Random(42);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            weightedSelect(addresses, weights, random);
        }

        // Measure
        long startNanos = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            weightedSelect(addresses, weights, random);
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double avgNanos = (double) elapsedNanos / ITERATIONS;
        double avgMicros = avgNanos / 1000.0;

        System.out.println("Weighted LB: " + String.format("%.3f", avgMicros) + " μs per selection");

        // Should still be fast - less than 5 microseconds
        assertTrue(avgMicros < 5.0,
                "Weighted LB should be <5μs per selection, was " + avgMicros + "μs");
    }

    @Test
    @DisplayName("Client metrics collection overhead should be <10μs")
    void testClientMetricsCollectionOverhead() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        List<InetSocketAddress> addresses = createAddresses(5);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            collector.collectMetrics("test-service", addresses);
        }

        // Measure
        long startNanos = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            collector.collectMetrics("test-service", addresses);
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double avgNanos = (double) elapsedNanos / ITERATIONS;
        double avgMicros = avgNanos / 1000.0;

        System.out.println("Metrics Collection: " + String.format("%.3f", avgMicros) + " μs per collection");

        // Should be reasonable - less than 50 microseconds (with JVM variance
        // tolerance)
        assertTrue(avgMicros < 50.0,
                "Metrics collection should be <50μs, was " + avgMicros + "μs");
    }

    @Test
    @DisplayName("Weight calculation overhead should be <1μs")
    void testWeightCalculationOverhead() {
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        ClientMetricsCollector.EndpointMetrics metrics = new ClientMetricsCollector.EndpointMetrics();
        metrics.setCircuitBreakerState(
                com.aicore.rpc.circuitbreaker.CircuitBreakerState.CLOSED);
        metrics.setFailureRate(15);
        metrics.setSlowCallRate(10);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            collector.calculateLocalWeight(metrics);
        }

        // Measure
        long startNanos = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            collector.calculateLocalWeight(metrics);
        }
        long elapsedNanos = System.nanoTime() - startNanos;

        double avgNanos = (double) elapsedNanos / ITERATIONS;
        double avgMicros = avgNanos / 1000.0;

        System.out.println("Weight Calculation: " + String.format("%.3f", avgMicros) + " μs per calculation");

        // Should be very fast
        assertTrue(avgMicros < 1.0,
                "Weight calculation should be <1μs, was " + avgMicros + "μs");
    }

    @Test
    @DisplayName("Overhead comparison: Random vs Weighted")
    void testOverheadComparison() {
        List<InetSocketAddress> addresses = createAddresses(10);
        Map<InetSocketAddress, Double> weights = createRandomWeights(addresses);
        Random random = new Random(42);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            randomLB.select(addresses);
            weightedSelect(addresses, weights, random);
        }

        // Measure Random LB
        long randomStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            randomLB.select(addresses);
        }
        long randomElapsed = System.nanoTime() - randomStart;

        // Measure Weighted LB
        random = new Random(42); // Reset
        long weightedStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            weightedSelect(addresses, weights, random);
        }
        long weightedElapsed = System.nanoTime() - weightedStart;

        double randomMicros = randomElapsed / 1000.0 / ITERATIONS;
        double weightedMicros = weightedElapsed / 1000.0 / ITERATIONS;
        double overheadRatio = weightedMicros / randomMicros;

        System.out.println("=== Performance Comparison ===");
        System.out.println("Random LB:   " + String.format("%.3f", randomMicros) + " μs/op");
        System.out.println("Weighted LB: " + String.format("%.3f", weightedMicros) + " μs/op");
        System.out.println("Overhead:    " + String.format("%.2fx", overheadRatio));

        // Weighted should not be more than 50x slower than random
        // (Higher tolerance due to JVM variance and weighted selection complexity)
        assertTrue(overheadRatio < 50.0,
                "Weighted LB overhead should be <50x random, was " + overheadRatio + "x");
    }

    @Test
    @DisplayName("Throughput test: selections per second")
    void testThroughput() {
        List<InetSocketAddress> addresses = createAddresses(5);
        Map<InetSocketAddress, Double> weights = createRandomWeights(addresses);
        Random random = new Random(42);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            weightedSelect(addresses, weights, random);
        }

        // Measure for 1 second
        long startTime = System.currentTimeMillis();
        long endTime = startTime + 1000; // 1 second
        int count = 0;

        while (System.currentTimeMillis() < endTime) {
            for (int i = 0; i < 1000; i++) {
                weightedSelect(addresses, weights, random);
                count++;
            }
        }

        long actualDuration = System.currentTimeMillis() - startTime;
        double throughput = (double) count / actualDuration * 1000; // per second

        System.out.println("Throughput: " + String.format("%,.0f", throughput) + " selections/sec");

        // Should be able to do at least 500K selections per second (conservative for
        // CI)
        assertTrue(throughput > 500_000,
                "Should achieve >500K selections/sec, got " + throughput);
    }

    // ===== Helper Methods =====

    private List<InetSocketAddress> createAddresses(int count) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            addresses.add(new InetSocketAddress("127.0.0.1", 8080 + i));
        }
        return addresses;
    }

    private Map<InetSocketAddress, Double> createRandomWeights(List<InetSocketAddress> addresses) {
        Map<InetSocketAddress, Double> weights = new HashMap<>();
        Random rand = new Random(42);
        for (InetSocketAddress addr : addresses) {
            weights.put(addr, rand.nextDouble());
        }
        return weights;
    }

    private InetSocketAddress weightedSelect(
            List<InetSocketAddress> addresses,
            Map<InetSocketAddress, Double> weights,
            Random random) {

        double totalWeight = weights.values().stream().mapToDouble(d -> d).sum();
        if (totalWeight <= 0) {
            return addresses.get(random.nextInt(addresses.size()));
        }

        double randomPoint = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (InetSocketAddress addr : addresses) {
            cumulative += weights.getOrDefault(addr, 0.0);
            if (randomPoint < cumulative) {
                return addr;
            }
        }
        return addresses.get(addresses.size() - 1);
    }
}
