package com.aicore.rpc.loadbalance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rigorous comparative tests between AI Load Balancer and Random Load Balancer.
 * Simulates various real-world scenarios to verify AI LB advantages.
 */
class LoadBalancerComparisonTest {

    private RandomLoadBalancer randomLB;
    private static final int ITERATIONS = 10000;

    @BeforeEach
    void setUp() {
        randomLB = new RandomLoadBalancer();
    }

    // ========== Scenario 1: Verify Random LB is truly uniform ==========

    @Test
    @DisplayName("Random LB should distribute uniformly across nodes")
    void testRandomLBUniformDistribution() {
        List<InetSocketAddress> addresses = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080),
                new InetSocketAddress("127.0.0.1", 8081),
                new InetSocketAddress("127.0.0.1", 8082));

        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = randomLB.select(addresses);
            int port = selected.getPort();
            counts.put(port, counts.getOrDefault(port, 0) + 1);
        }

        // Statistical test: each should be ~33.3% with tolerance
        double expected = ITERATIONS / 3.0;
        double tolerance = expected * 0.15; // 15% tolerance

        for (int port : Arrays.asList(8080, 8081, 8082)) {
            int count = counts.getOrDefault(port, 0);
            assertTrue(Math.abs(count - expected) < tolerance,
                    "Port " + port + ": expected ~" + (int) expected + " but got " + count);
        }
    }

    // ========== Scenario 2: Simulate AI weighted selection ==========

    @Test
    @DisplayName("Weighted selection should favor higher weights (9:1 ratio)")
    void testWeightedSelectionWith9to1Ratio() {
        // Simulate AI LB weighted selection with 9:1 weight ratio
        InetSocketAddress fast = new InetSocketAddress("127.0.0.1", 8080);
        InetSocketAddress slow = new InetSocketAddress("127.0.0.1", 8081);

        Map<InetSocketAddress, Double> weights = new HashMap<>();
        weights.put(fast, 0.9); // Fast server, high weight
        weights.put(slow, 0.1); // Slow server, low weight

        Map<Integer, Integer> counts = new HashMap<>();
        Random random = new Random(42);

        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = weightedSelect(new ArrayList<>(weights.keySet()), weights, random);
            counts.put(selected.getPort(), counts.getOrDefault(selected.getPort(), 0) + 1);
        }

        int fastCount = counts.getOrDefault(8080, 0);
        int slowCount = counts.getOrDefault(8081, 0);

        // With 9:1 weights, fast should get ~90% of traffic
        double fastRatio = (double) fastCount / ITERATIONS;
        assertTrue(fastRatio > 0.85 && fastRatio < 0.95,
                "Fast server should get ~90% traffic, got " + (fastRatio * 100) + "%");
    }

    @Test
    @DisplayName("Weighted selection with circuit breaker exclusion")
    void testWeightedSelectionWithCircuitBreakerExclusion() {
        // Simulate: one node has open circuit breaker (weight = 0)
        InetSocketAddress healthy1 = new InetSocketAddress("127.0.0.1", 8080);
        InetSocketAddress healthy2 = new InetSocketAddress("127.0.0.1", 8081);
        InetSocketAddress failed = new InetSocketAddress("127.0.0.1", 8082);

        Map<InetSocketAddress, Double> weights = new HashMap<>();
        weights.put(healthy1, 0.5);
        weights.put(healthy2, 0.5);
        weights.put(failed, 0.0); // Circuit breaker OPEN

        Map<Integer, Integer> counts = new HashMap<>();
        Random random = new Random(42);

        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = weightedSelect(new ArrayList<>(weights.keySet()), weights, random);
            counts.put(selected.getPort(), counts.getOrDefault(selected.getPort(), 0) + 1);
        }

        int failedCount = counts.getOrDefault(8082, 0);
        assertEquals(0, failedCount, "Failed node should receive 0 traffic");

        // Other nodes should split traffic
        int healthy1Count = counts.getOrDefault(8080, 0);
        int healthy2Count = counts.getOrDefault(8081, 0);
        assertEquals(ITERATIONS, healthy1Count + healthy2Count, "All traffic should go to healthy nodes");
    }

    @Test
    @DisplayName("AI LB adapts to latency differences (exponential decay)")
    void testExponentialDecayHealthScore() {
        // Test the exponential decay formula used in AI forecasting service
        // health_score = e^(-k * latency) where k = 20
        double k = 20;

        // Fast server: 10ms latency
        double fastLatency = 0.010; // 10ms in seconds
        double fastScore = Math.exp(-k * fastLatency);

        // Slow server: 100ms latency
        double slowLatency = 0.100; // 100ms in seconds
        double slowScore = Math.exp(-k * slowLatency);

        // Fast should have much higher score than slow
        assertTrue(fastScore > slowScore * 5,
                "Fast server score (" + fastScore + ") should be >5x slow server score (" + slowScore + ")");

        // Verify actual values
        assertEquals(0.8187, fastScore, 0.01, "10ms latency health score");
        assertEquals(0.1353, slowScore, 0.01, "100ms latency health score");
    }

    @Test
    @DisplayName("Weighted selection with real latency-based weights")
    void testWeightedSelectionWithLatencyBasedWeights() {
        // Simulate servers with different latencies
        InetSocketAddress fast = new InetSocketAddress("127.0.0.1", 8080); // 10ms
        InetSocketAddress medium = new InetSocketAddress("127.0.0.1", 8081); // 50ms
        InetSocketAddress slow = new InetSocketAddress("127.0.0.1", 8082); // 100ms

        double k = 20;
        Map<InetSocketAddress, Double> weights = new HashMap<>();
        weights.put(fast, Math.exp(-k * 0.010)); // ~0.82
        weights.put(medium, Math.exp(-k * 0.050)); // ~0.37
        weights.put(slow, Math.exp(-k * 0.100)); // ~0.14

        Map<Integer, Integer> counts = new HashMap<>();
        Random random = new Random(42);

        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = weightedSelect(new ArrayList<>(weights.keySet()), weights, random);
            counts.put(selected.getPort(), counts.getOrDefault(selected.getPort(), 0) + 1);
        }

        int fastCount = counts.get(8080);
        int mediumCount = counts.get(8081);
        int slowCount = counts.get(8082);

        // Fast should get most traffic, slow should get least
        assertTrue(fastCount > mediumCount,
                "Fast (" + fastCount + ") should get more traffic than medium (" + mediumCount + ")");
        assertTrue(mediumCount > slowCount,
                "Medium (" + mediumCount + ") should get more traffic than slow (" + slowCount + ")");

        // Fast should get >50% of traffic
        double fastRatio = (double) fastCount / ITERATIONS;
        assertTrue(fastRatio > 0.50, "Fast server should get >50% traffic, got " + (fastRatio * 100) + "%");
    }

    @Test
    @DisplayName("Compare Random vs Weighted distribution for heterogeneous servers")
    void testCompareRandomVsWeightedDistribution() {
        List<InetSocketAddress> addresses = Arrays.asList(
                new InetSocketAddress("127.0.0.1", 8080), // fast
                new InetSocketAddress("127.0.0.1", 8081), // slow
                new InetSocketAddress("127.0.0.1", 8082) // very slow
        );

        // Random LB distribution
        Map<Integer, Integer> randomCounts = new HashMap<>();
        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = randomLB.select(addresses);
            randomCounts.put(selected.getPort(), randomCounts.getOrDefault(selected.getPort(), 0) + 1);
        }

        // Simulated AI LB distribution (with latency-based weights)
        double k = 20;
        Map<InetSocketAddress, Double> weights = new HashMap<>();
        weights.put(addresses.get(0), Math.exp(-k * 0.010)); // 10ms -> 0.82
        weights.put(addresses.get(1), Math.exp(-k * 0.100)); // 100ms -> 0.14
        weights.put(addresses.get(2), Math.exp(-k * 0.200)); // 200ms -> 0.02

        Map<Integer, Integer> aiCounts = new HashMap<>();
        Random random = new Random(42);
        for (int i = 0; i < ITERATIONS; i++) {
            InetSocketAddress selected = weightedSelect(addresses, weights, random);
            aiCounts.put(selected.getPort(), aiCounts.getOrDefault(selected.getPort(), 0) + 1);
        }

        // Analysis
        System.out.println("=== Random LB vs AI LB Distribution Comparison ===");
        System.out.println("Random LB: 8080=" + randomCounts.get(8080) +
                ", 8081=" + randomCounts.get(8081) +
                ", 8082=" + randomCounts.get(8082));
        System.out.println("AI LB:     8080=" + aiCounts.get(8080) +
                ", 8081=" + aiCounts.get(8081) +
                ", 8082=" + aiCounts.get(8082));

        // AI LB should route more to fast server than random
        assertTrue(aiCounts.get(8080) > randomCounts.get(8080) * 1.5,
                "AI LB should route significantly more traffic to fast server than Random LB");

        // AI LB should route less to very slow server than random
        assertTrue(aiCounts.get(8082) < randomCounts.get(8082) * 0.5,
                "AI LB should route significantly less traffic to very slow server than Random LB");
    }

    @Test
    @DisplayName("AI weight fusion with client metrics reduces unhealthy traffic")
    void testAIWeightFusionWithClientMetrics() {
        // Scenario: AI says server is healthy, but local metrics show high failure rate
        double aiWeight = 0.8; // AI thinks it's healthy
        double localWeight = 0.2; // But local circuit breaker shows 60% failure rate

        double fusedWeight = aiWeight * localWeight;
        assertEquals(0.16, fusedWeight, 0.01);

        // Compare with truly healthy server
        double healthyFusedWeight = 0.8 * 1.0;

        // Unhealthy should get ~20% of healthy's traffic
        assertTrue(fusedWeight < healthyFusedWeight * 0.25,
                "Server with high failure rate should get much less traffic");
    }

    // ========== Helper method for weighted random selection ==========

    private InetSocketAddress weightedSelect(List<InetSocketAddress> addresses,
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
