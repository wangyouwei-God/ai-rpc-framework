package com.aicore.rpc.loadbalance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.net.InetSocketAddress;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Level 4: Edge Cases and Boundary Condition Tests.
 * Tests the system's behavior under extreme or unusual conditions.
 */
class EdgeCaseTest {

    private RandomLoadBalancer randomLB;

    @BeforeEach
    void setUp() {
        randomLB = new RandomLoadBalancer();
    }

    // ===== A. Empty/Null Input Cases =====

    @Nested
    @DisplayName("A. Empty/Null Input Cases")
    class EmptyNullInputTests {

        @Test
        @DisplayName("Random LB should return null for empty list")
        void testRandomLBEmptyList() {
            InetSocketAddress result = randomLB.select(Collections.emptyList());
            assertNull(result, "Should return null for empty list");
        }

        @Test
        @DisplayName("Random LB should return null for null input")
        void testRandomLBNullInput() {
            InetSocketAddress result = randomLB.select(null);
            assertNull(result, "Should return null for null input");
        }

        @Test
        @DisplayName("Weighted select should handle empty weights gracefully")
        void testWeightedSelectEmptyWeights() {
            List<InetSocketAddress> addresses = Arrays.asList(
                    new InetSocketAddress("127.0.0.1", 8080),
                    new InetSocketAddress("127.0.0.1", 8081));
            Map<InetSocketAddress, Double> emptyWeights = new HashMap<>();

            // Should fallback to random selection
            Random random = new Random(42);
            InetSocketAddress result = weightedSelectWithFallback(addresses, emptyWeights, random);
            assertNotNull(result, "Should fallback to random selection");
            assertTrue(addresses.contains(result), "Should select from available addresses");
        }
    }

    // ===== B. Single Node Cases =====

    @Nested
    @DisplayName("B. Single Node Cases")
    class SingleNodeTests {

        @Test
        @DisplayName("Random LB should return the only node")
        void testRandomLBSingleNode() {
            List<InetSocketAddress> addresses = Collections.singletonList(
                    new InetSocketAddress("127.0.0.1", 8080));

            for (int i = 0; i < 100; i++) {
                InetSocketAddress result = randomLB.select(addresses);
                assertEquals(8080, result.getPort(), "Should always return the only node");
            }
        }

        @Test
        @DisplayName("Weighted LB should return the only node regardless of weight")
        void testWeightedLBSingleNode() {
            InetSocketAddress onlyNode = new InetSocketAddress("127.0.0.1", 8080);
            List<InetSocketAddress> addresses = Collections.singletonList(onlyNode);
            Map<InetSocketAddress, Double> weights = new HashMap<>();
            weights.put(onlyNode, 0.1); // Very low weight

            Random random = new Random(42);
            for (int i = 0; i < 100; i++) {
                InetSocketAddress result = weightedSelectWithFallback(addresses, weights, random);
                assertEquals(onlyNode, result, "Should always return the only node");
            }
        }
    }

    // ===== C. All Nodes Unhealthy =====

    @Nested
    @DisplayName("C. All Nodes Unhealthy (Weight=0)")
    class AllNodesUnhealthyTests {

        @Test
        @DisplayName("Should fallback to random when all weights are zero")
        void testAllWeightsZero() {
            List<InetSocketAddress> addresses = Arrays.asList(
                    new InetSocketAddress("127.0.0.1", 8080),
                    new InetSocketAddress("127.0.0.1", 8081),
                    new InetSocketAddress("127.0.0.1", 8082));

            Map<InetSocketAddress, Double> weights = new HashMap<>();
            for (InetSocketAddress addr : addresses) {
                weights.put(addr, 0.0); // All circuit breakers OPEN
            }

            Map<Integer, Integer> counts = new HashMap<>();
            Random random = new Random(42);

            for (int i = 0; i < 1000; i++) {
                InetSocketAddress result = weightedSelectWithFallback(addresses, weights, random);
                assertNotNull(result, "Should fallback to random selection");
                counts.put(result.getPort(), counts.getOrDefault(result.getPort(), 0) + 1);
            }

            // Verify distribution is roughly uniform (fallback to random)
            for (int port : Arrays.asList(8080, 8081, 8082)) {
                int count = counts.getOrDefault(port, 0);
                assertTrue(count > 200 && count < 500,
                        "Port " + port + " should have ~333 calls (random fallback), got " + count);
            }
        }
    }

    // ===== D. Extreme Weight Ratios =====

    @Nested
    @DisplayName("D. Extreme Weight Ratios")
    class ExtremeWeightTests {

        @Test
        @DisplayName("One node with near-zero weight should rarely be selected")
        void testNearZeroWeight() {
            InetSocketAddress healthy = new InetSocketAddress("127.0.0.1", 8080);
            InetSocketAddress degraded = new InetSocketAddress("127.0.0.1", 8081);

            List<InetSocketAddress> addresses = Arrays.asList(healthy, degraded);
            Map<InetSocketAddress, Double> weights = new HashMap<>();
            weights.put(healthy, 1.0);
            weights.put(degraded, 0.001); // Near-zero weight

            Map<Integer, Integer> counts = new HashMap<>();
            Random random = new Random(42);

            for (int i = 0; i < 10000; i++) {
                InetSocketAddress result = weightedSelectWithFallback(addresses, weights, random);
                counts.put(result.getPort(), counts.getOrDefault(result.getPort(), 0) + 1);
            }

            int healthyCount = counts.getOrDefault(8080, 0);
            int degradedCount = counts.getOrDefault(8081, 0);

            // Healthy should get >99% of traffic
            assertTrue(healthyCount > 9900,
                    "Healthy node should get >99% traffic, got " + (healthyCount / 100.0) + "%");
            assertTrue(degradedCount < 100,
                    "Degraded node should get <1% traffic, got " + (degradedCount / 100.0) + "%");
        }

        @Test
        @DisplayName("Very high weight ratio (1000:1) should be correctly distributed")
        void testVeryHighWeightRatio() {
            InetSocketAddress heavy = new InetSocketAddress("127.0.0.1", 8080);
            InetSocketAddress light = new InetSocketAddress("127.0.0.1", 8081);

            List<InetSocketAddress> addresses = Arrays.asList(heavy, light);
            Map<InetSocketAddress, Double> weights = new HashMap<>();
            weights.put(heavy, 1000.0);
            weights.put(light, 1.0);

            Map<Integer, Integer> counts = new HashMap<>();
            Random random = new Random(42);

            for (int i = 0; i < 10000; i++) {
                InetSocketAddress result = weightedSelectWithFallback(addresses, weights, random);
                counts.put(result.getPort(), counts.getOrDefault(result.getPort(), 0) + 1);
            }

            double heavyRatio = counts.getOrDefault(8080, 0) / 10000.0;
            double lightRatio = counts.getOrDefault(8081, 0) / 10000.0;

            // Heavy should get ~99.9% (1000/1001)
            assertTrue(heavyRatio > 0.99, "Heavy should get >99%, got " + (heavyRatio * 100) + "%");
            assertTrue(lightRatio < 0.01, "Light should get <1%, got " + (lightRatio * 100) + "%");
        }
    }

    // ===== E. Dynamic Changes =====

    @Nested
    @DisplayName("E. Dynamic Changes Simulation")
    class DynamicChangesTests {

        @Test
        @DisplayName("Adding a new high-weight node should shift traffic")
        void testAddingNewHighWeightNode() {
            InetSocketAddress existing = new InetSocketAddress("127.0.0.1", 8080);
            InetSocketAddress newNode = new InetSocketAddress("127.0.0.1", 8081);

            // Phase 1: Only one node
            List<InetSocketAddress> phase1Addresses = Collections.singletonList(existing);
            Map<InetSocketAddress, Double> phase1Weights = new HashMap<>();
            phase1Weights.put(existing, 0.5);

            Random random = new Random(42);
            int phase1Count = countSelections(phase1Addresses, phase1Weights, existing, random, 1000);
            assertEquals(1000, phase1Count, "Should always select the only node");

            // Phase 2: Add a faster node
            List<InetSocketAddress> phase2Addresses = Arrays.asList(existing, newNode);
            Map<InetSocketAddress, Double> phase2Weights = new HashMap<>();
            phase2Weights.put(existing, 0.3); // Slightly degraded
            phase2Weights.put(newNode, 0.9); // New fast node

            int existingPhase2 = countSelections(phase2Addresses, phase2Weights, existing, random, 1000);
            int newNodePhase2 = countSelections(phase2Addresses, phase2Weights, newNode, random, 1000);

            assertTrue(newNodePhase2 > existingPhase2 * 2,
                    "New fast node should get much more traffic");
        }

        @Test
        @DisplayName("Degrading node weight should shift traffic away")
        void testDegradingNodeWeight() {
            InetSocketAddress nodeA = new InetSocketAddress("127.0.0.1", 8080);
            InetSocketAddress nodeB = new InetSocketAddress("127.0.0.1", 8081);
            List<InetSocketAddress> addresses = Arrays.asList(nodeA, nodeB);

            // Phase 1: Both healthy
            Map<InetSocketAddress, Double> phase1Weights = new HashMap<>();
            phase1Weights.put(nodeA, 0.8);
            phase1Weights.put(nodeB, 0.8);

            Random random = new Random(42);
            int nodeAPhase1 = countSelections(addresses, phase1Weights, nodeA, random, 1000);
            // Should be roughly 50/50
            assertTrue(nodeAPhase1 > 400 && nodeAPhase1 < 600,
                    "Should be roughly equal, got " + nodeAPhase1);

            // Phase 2: Node A becomes unhealthy
            Map<InetSocketAddress, Double> phase2Weights = new HashMap<>();
            phase2Weights.put(nodeA, 0.1); // Degraded
            phase2Weights.put(nodeB, 0.8); // Still healthy

            random = new Random(42);
            int nodeAPhase2 = countSelections(addresses, phase2Weights, nodeA, random, 1000);

            assertTrue(nodeAPhase2 < 200,
                    "Degraded node should get <20% traffic, got " + (nodeAPhase2 / 10.0) + "%");
        }
    }

    // ===== Helper Methods =====

    private InetSocketAddress weightedSelectWithFallback(
            List<InetSocketAddress> addresses,
            Map<InetSocketAddress, Double> weights,
            Random random) {

        if (addresses == null || addresses.isEmpty()) {
            return null;
        }

        double totalWeight = weights.values().stream().mapToDouble(d -> d).sum();

        // Fallback to random if all weights are zero
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

    private int countSelections(
            List<InetSocketAddress> addresses,
            Map<InetSocketAddress, Double> weights,
            InetSocketAddress target,
            Random random,
            int iterations) {

        int count = 0;
        for (int i = 0; i < iterations; i++) {
            InetSocketAddress selected = weightedSelectWithFallback(addresses, weights, random);
            if (target.equals(selected)) {
                count++;
            }
        }
        return count;
    }
}
