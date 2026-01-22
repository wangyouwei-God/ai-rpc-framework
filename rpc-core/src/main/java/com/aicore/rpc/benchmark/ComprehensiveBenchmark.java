package com.aicore.rpc.benchmark;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * Comprehensive 20-Minute Performance Benchmark
 * 
 * Professional-grade benchmark comparing AI-Predictive Load Balancing
 * against traditional algorithms under various realistic scenarios.
 * 
 * Phases:
 * 1. Warmup (1 min) - JVM warmup
 * 2. Normal (4 min) - Heterogeneous nodes
 * 3. High Load (4 min) - 50 concurrent threads
 * 4. Node Failure (4 min) - One node degrades
 * 5. Recovery (3 min) - Failed node recovers
 * 6. Chaos (4 min) - Random latency fluctuations
 * 
 * @author AI-RPC Framework Team
 */
public class ComprehensiveBenchmark {

    // ==================== CONFIGURATION ====================

    private static final int WARMUP_DURATION_SEC = 60;
    private static final int PHASE_1_DURATION_SEC = 240; // 4 min - Normal
    private static final int PHASE_2_DURATION_SEC = 240; // 4 min - High Load
    private static final int PHASE_3_DURATION_SEC = 240; // 4 min - Node Failure
    private static final int PHASE_4_DURATION_SEC = 180; // 3 min - Recovery
    private static final int PHASE_5_DURATION_SEC = 240; // 4 min - Chaos

    private static final int NORMAL_CONCURRENCY = 20;
    private static final int HIGH_CONCURRENCY = 50;
    private static final int METRICS_INTERVAL_SEC = 10;

    // Server configurations
    private static final List<InetSocketAddress> NODES = Arrays.asList(
            new InetSocketAddress("127.0.0.1", 8080),
            new InetSocketAddress("127.0.0.1", 8081),
            new InetSocketAddress("127.0.0.1", 8082));

    // Algorithms to test
    private static final String[] ALGORITHMS = { "Random", "RoundRobin", "WeightedStatic", "AI" };

    // ==================== STATE ====================

    private final Map<InetSocketAddress, AtomicInteger> nodeLatencies = new ConcurrentHashMap<>();
    private final Map<String, List<PhaseMetrics>> allResults = new LinkedHashMap<>();
    private final Random random = new Random(42);
    private volatile String currentPhase = "Warmup";
    private volatile int currentConcurrency = NORMAL_CONCURRENCY;
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    // ==================== MAIN ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║     AI-RPC Framework - Comprehensive Performance Benchmark          ║");
        System.out.println("║                      20-Minute Professional Test                     ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        ComprehensiveBenchmark benchmark = new ComprehensiveBenchmark();
        benchmark.run();
    }

    public void run() {
        long startTime = System.currentTimeMillis();

        // Initialize
        for (InetSocketAddress node : NODES) {
            nodeLatencies.put(node, new AtomicInteger(50));
        }
        for (String algo : ALGORITHMS) {
            allResults.put(algo, new ArrayList<>());
        }

        try {
            // Phase 0: Warmup
            runPhase("Warmup", WARMUP_DURATION_SEC, () -> configureNodes(10, 50, 200));

            // Phase 1: Normal Load
            runPhase("Phase1_Normal", PHASE_1_DURATION_SEC, () -> configureNodes(10, 50, 200));

            // Phase 2: High Load
            currentConcurrency = HIGH_CONCURRENCY;
            runPhase("Phase2_HighLoad", PHASE_2_DURATION_SEC, () -> configureNodes(10, 50, 200));
            currentConcurrency = NORMAL_CONCURRENCY;

            // Phase 3: Node Failure
            runPhase("Phase3_NodeFailure", PHASE_3_DURATION_SEC, () -> configureNodes(10, 50, 500));

            // Phase 4: Recovery
            runPhase("Phase4_Recovery", PHASE_4_DURATION_SEC, () -> configureNodes(10, 50, 200));

            // Phase 5: Chaos
            runPhase("Phase5_Chaos", PHASE_5_DURATION_SEC, this::applyChaoticLatencies);

        } catch (Exception e) {
            System.err.println("Benchmark failed: " + e.getMessage());
            e.printStackTrace();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\n══════════════════════════════════════════════════════════════════════");
        System.out.println("Benchmark completed in " + (totalTime / 1000) + " seconds");
        System.out.println("══════════════════════════════════════════════════════════════════════\n");

        // Generate reports
        generateReport();
        exportToCSV();
    }

    // ==================== PHASE EXECUTION ====================

    private void runPhase(String phaseName, int durationSec, Runnable latencyConfig) {
        currentPhase = phaseName;
        System.out.println("\n┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println(
                "│ " + String.format("%-68s", phaseName + " (" + durationSec + "s, " + currentConcurrency + " threads)")
                        + " │");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");

        latencyConfig.run();
        printCurrentLatencies();

        int intervals = durationSec / METRICS_INTERVAL_SEC;

        for (int i = 0; i < intervals; i++) {
            // Update chaos latencies if in chaos phase
            if (phaseName.contains("Chaos") && i % 2 == 0) {
                latencyConfig.run();
            }

            // Run all algorithms for this interval
            for (String algo : ALGORITHMS) {
                PhaseMetrics metrics = runInterval(algo, METRICS_INTERVAL_SEC);
                metrics.phase = phaseName;

                // Only record after warmup
                if (!phaseName.equals("Warmup")) {
                    allResults.get(algo).add(metrics);
                }
            }

            // Progress indicator
            int elapsed = (i + 1) * METRICS_INTERVAL_SEC;
            System.out.printf("  Progress: %d/%d sec (%.0f%%)%n",
                    elapsed, durationSec, (elapsed * 100.0 / durationSec));
        }
    }

    private PhaseMetrics runInterval(String algorithm, int durationSec) {
        ExecutorService executor = Executors.newFixedThreadPool(currentConcurrency);
        PhaseMetrics metrics = new PhaseMetrics();
        metrics.algorithm = algorithm;
        metrics.concurrency = currentConcurrency;
        metrics.timestamp = System.currentTimeMillis();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSec * 1000L);

        List<Future<RequestResult>> futures = new ArrayList<>();

        while (System.currentTimeMillis() < endTime) {
            Future<RequestResult> future = executor.submit(() -> {
                InetSocketAddress selected = selectNode(algorithm);
                int latency = nodeLatencies.get(selected).get();

                // Simulate latency with jitter
                int jitter = random.nextInt(Math.max(1, latency / 10));
                int actualLatency = latency + jitter - (latency / 20);

                try {
                    Thread.sleep(Math.max(1, actualLatency));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return new RequestResult(selected, actualLatency, false);
            });
            futures.add(future);

            // Small delay between submissions
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Collect results
        List<Integer> latencies = new ArrayList<>();
        Map<InetSocketAddress, Integer> nodeCounts = new HashMap<>();
        for (InetSocketAddress node : NODES) {
            nodeCounts.put(node, 0);
        }

        for (Future<RequestResult> future : futures) {
            try {
                RequestResult result = future.get();
                latencies.add(result.latency);
                nodeCounts.put(result.node, nodeCounts.get(result.node) + 1);
            } catch (Exception e) {
                metrics.errors++;
            }
        }

        // Calculate metrics
        metrics.requestCount = latencies.size();
        metrics.throughput = metrics.requestCount / (double) durationSec;

        if (!latencies.isEmpty()) {
            Collections.sort(latencies);
            metrics.avgLatency = latencies.stream().mapToInt(i -> i).average().orElse(0);
            metrics.minLatency = latencies.get(0);
            metrics.maxLatency = latencies.get(latencies.size() - 1);
            metrics.p50 = percentile(latencies, 50);
            metrics.p90 = percentile(latencies, 90);
            metrics.p95 = percentile(latencies, 95);
            metrics.p99 = percentile(latencies, 99);
        }

        // Node distribution
        int total = nodeCounts.values().stream().mapToInt(i -> i).sum();
        if (total > 0) {
            metrics.fastNodeRatio = nodeCounts.get(NODES.get(0)) / (double) total;
            metrics.mediumNodeRatio = nodeCounts.get(NODES.get(1)) / (double) total;
            metrics.slowNodeRatio = nodeCounts.get(NODES.get(2)) / (double) total;
        }

        return metrics;
    }

    // ==================== LOAD BALANCER SELECTION ====================

    private InetSocketAddress selectNode(String algorithm) {
        switch (algorithm) {
            case "Random":
                return NODES.get(random.nextInt(NODES.size()));
            case "RoundRobin":
                return NODES.get(roundRobinCounter.getAndIncrement() % NODES.size());
            case "WeightedStatic":
                return selectWeightedStatic();
            case "AI":
                return selectAI();
            default:
                return NODES.get(0);
        }
    }

    private InetSocketAddress selectWeightedStatic() {
        double[] weights = { 0.5, 0.33, 0.17 };
        double rand = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < NODES.size(); i++) {
            cumulative += weights[i];
            if (rand < cumulative)
                return NODES.get(i);
        }
        return NODES.get(NODES.size() - 1);
    }

    private InetSocketAddress selectAI() {
        // AI selection based on latency-derived health scores
        Map<InetSocketAddress, Double> weights = new HashMap<>();
        double totalWeight = 0;

        for (InetSocketAddress node : NODES) {
            int latency = nodeLatencies.get(node).get();
            // Exponential decay health score
            double score = Math.exp(-0.02 * latency);
            weights.put(node, score);
            totalWeight += score;
        }

        double rand = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (InetSocketAddress node : NODES) {
            cumulative += weights.get(node);
            if (rand < cumulative)
                return node;
        }
        return NODES.get(NODES.size() - 1);
    }

    // ==================== LATENCY CONFIGURATION ====================

    private void configureNodes(int lat1, int lat2, int lat3) {
        nodeLatencies.get(NODES.get(0)).set(lat1);
        nodeLatencies.get(NODES.get(1)).set(lat2);
        nodeLatencies.get(NODES.get(2)).set(lat3);
    }

    private void applyChaoticLatencies() {
        // Random latencies between 5-300ms
        for (InetSocketAddress node : NODES) {
            int latency = 5 + random.nextInt(295);
            nodeLatencies.get(node).set(latency);
        }
    }

    private void printCurrentLatencies() {
        System.out.print("  Node latencies: ");
        for (int i = 0; i < NODES.size(); i++) {
            System.out.print("Node" + i + "=" + nodeLatencies.get(NODES.get(i)).get() + "ms ");
        }
        System.out.println();
    }

    // ==================== STATISTICS ====================

    private int percentile(List<Integer> sorted, int p) {
        if (sorted.isEmpty())
            return 0;
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    // ==================== REPORT GENERATION ====================

    private void generateReport() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    BENCHMARK RESULTS SUMMARY                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");

        // Group by phase
        Map<String, Map<String, List<PhaseMetrics>>> byPhase = new LinkedHashMap<>();
        for (String algo : ALGORITHMS) {
            for (PhaseMetrics m : allResults.get(algo)) {
                byPhase.computeIfAbsent(m.phase, k -> new LinkedHashMap<>())
                        .computeIfAbsent(algo, k -> new ArrayList<>())
                        .add(m);
            }
        }

        // Print summary for each phase
        for (Map.Entry<String, Map<String, List<PhaseMetrics>>> phaseEntry : byPhase.entrySet()) {
            String phase = phaseEntry.getKey();
            Map<String, List<PhaseMetrics>> algoMetrics = phaseEntry.getValue();

            System.out.println("\n┌─ " + phase + " ─────────────────────────────────────────────────────────┐");
            System.out.println("│ Algorithm      │ Throughput │ Avg Lat │  P50  │  P99  │ Fast% │ Slow% │");
            System.out.println("├────────────────┼────────────┼─────────┼───────┼───────┼───────┼───────┤");

            for (String algo : ALGORITHMS) {
                List<PhaseMetrics> metrics = algoMetrics.getOrDefault(algo, Collections.emptyList());
                if (metrics.isEmpty())
                    continue;

                double avgThroughput = metrics.stream().mapToDouble(m -> m.throughput).average().orElse(0);
                double avgLatency = metrics.stream().mapToDouble(m -> m.avgLatency).average().orElse(0);
                double avgP50 = metrics.stream().mapToDouble(m -> m.p50).average().orElse(0);
                double avgP99 = metrics.stream().mapToDouble(m -> m.p99).average().orElse(0);
                double avgFast = metrics.stream().mapToDouble(m -> m.fastNodeRatio).average().orElse(0);
                double avgSlow = metrics.stream().mapToDouble(m -> m.slowNodeRatio).average().orElse(0);

                System.out.printf("│ %-14s │ %8.1f/s │ %5.1fms │ %5.0f │ %5.0f │ %5.1f │ %5.1f │%n",
                        algo, avgThroughput, avgLatency, avgP50, avgP99, avgFast * 100, avgSlow * 100);
            }
            System.out.println("└────────────────┴────────────┴─────────┴───────┴───────┴───────┴───────┘");
        }

        // Overall comparison
        printOverallComparison();
    }

    private void printOverallComparison() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      OVERALL COMPARISON                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════╝");

        System.out.println("\n│ Algorithm      │ Total Reqs │ Avg Lat │  P50  │  P99  │ Fast% │ Improvement │");
        System.out.println("├────────────────┼────────────┼─────────┼───────┼───────┼───────┼─────────────┤");

        double randomFast = 0;
        for (String algo : ALGORITHMS) {
            List<PhaseMetrics> metrics = allResults.get(algo);
            if (metrics.isEmpty())
                continue;

            long totalReqs = metrics.stream().mapToLong(m -> m.requestCount).sum();
            double avgLatency = metrics.stream().mapToDouble(m -> m.avgLatency).average().orElse(0);
            double avgP50 = metrics.stream().mapToDouble(m -> m.p50).average().orElse(0);
            double avgP99 = metrics.stream().mapToDouble(m -> m.p99).average().orElse(0);
            double avgFast = metrics.stream().mapToDouble(m -> m.fastNodeRatio).average().orElse(0);

            if (algo.equals("Random")) {
                randomFast = avgFast;
            }

            String improvement = algo.equals("Random") ? "-"
                    : String.format("%+.1f%%", (avgFast - randomFast) / randomFast * 100);

            System.out.printf("│ %-14s │ %10d │ %5.1fms │ %5.0f │ %5.0f │ %5.1f │ %11s │%n",
                    algo, totalReqs, avgLatency, avgP50, avgP99, avgFast * 100, improvement);
        }
        System.out.println("└────────────────┴────────────┴─────────┴───────┴───────┴───────┴─────────────┘");

        // Statistical significance
        printStatisticalAnalysis();
    }

    private void printStatisticalAnalysis() {
        System.out.println("\n┌─ Statistical Analysis ───────────────────────────────────────────────┐");

        List<Double> randomFast = allResults.get("Random").stream()
                .mapToDouble(m -> m.fastNodeRatio).boxed().collect(Collectors.toList());
        List<Double> aiFast = allResults.get("AI").stream()
                .mapToDouble(m -> m.fastNodeRatio).boxed().collect(Collectors.toList());

        if (randomFast.isEmpty() || aiFast.isEmpty()) {
            System.out.println("│ Insufficient data for statistical analysis                          │");
            System.out.println("└──────────────────────────────────────────────────────────────────────┘");
            return;
        }

        double randomMean = randomFast.stream().mapToDouble(d -> d).average().orElse(0);
        double aiMean = aiFast.stream().mapToDouble(d -> d).average().orElse(0);

        double randomStd = Math.sqrt(randomFast.stream()
                .mapToDouble(d -> Math.pow(d - randomMean, 2)).average().orElse(0));
        double aiStd = Math.sqrt(aiFast.stream()
                .mapToDouble(d -> Math.pow(d - aiMean, 2)).average().orElse(0));

        // T-test (simplified)
        double pooledStd = Math.sqrt((randomStd * randomStd + aiStd * aiStd) / 2);
        double tStat = (aiMean - randomMean) / (pooledStd * Math.sqrt(2.0 / randomFast.size()));

        // Cohen's d effect size
        double cohensD = (aiMean - randomMean) / pooledStd;

        String effectInterpretation = cohensD < 0.2 ? "Negligible"
                : cohensD < 0.5 ? "Small" : cohensD < 0.8 ? "Medium" : "Large";

        System.out.printf("│ Random Mean: %.3f ± %.3f                                           │%n", randomMean,
                randomStd);
        System.out.printf("│ AI Mean:     %.3f ± %.3f                                           │%n", aiMean, aiStd);
        System.out.printf("│ Difference:  %+.1f%% (AI vs Random)                                   │%n",
                (aiMean - randomMean) * 100);
        System.out.printf("│ T-statistic: %.2f                                                    │%n", tStat);
        System.out.printf("│ Cohen's d:   %.2f (%s effect)                                    │%n", cohensD,
                effectInterpretation);
        System.out.printf("│ Significant: %s (|t| > 2.0)                                          │%n",
                Math.abs(tStat) > 2.0 ? "✅ YES" : "❌ NO");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘");
    }

    private void exportToCSV() {
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now());
        String filename = "benchmark_results_" + timestamp + ".csv";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("Phase,Algorithm,Timestamp,Concurrency,RequestCount,Throughput," +
                    "AvgLatency,MinLatency,MaxLatency,P50,P90,P95,P99," +
                    "FastNodeRatio,MediumNodeRatio,SlowNodeRatio,Errors");

            // Data
            for (String algo : ALGORITHMS) {
                for (PhaseMetrics m : allResults.get(algo)) {
                    writer.printf("%s,%s,%d,%d,%d,%.2f,%.2f,%d,%d,%d,%d,%d,%d,%.4f,%.4f,%.4f,%d%n",
                            m.phase, m.algorithm, m.timestamp, m.concurrency, m.requestCount,
                            m.throughput, m.avgLatency, m.minLatency, m.maxLatency,
                            m.p50, m.p90, m.p95, m.p99,
                            m.fastNodeRatio, m.mediumNodeRatio, m.slowNodeRatio, m.errors);
                }
            }

            System.out.println("\n✅ Results exported to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to export CSV: " + e.getMessage());
        }
    }

    // ==================== INNER CLASSES ====================

    static class PhaseMetrics {
        String phase;
        String algorithm;
        long timestamp;
        int concurrency;
        int requestCount;
        double throughput;
        double avgLatency;
        int minLatency;
        int maxLatency;
        int p50, p90, p95, p99;
        double fastNodeRatio;
        double mediumNodeRatio;
        double slowNodeRatio;
        int errors;
    }

    static class RequestResult {
        InetSocketAddress node;
        int latency;
        boolean error;

        RequestResult(InetSocketAddress node, int latency, boolean error) {
            this.node = node;
            this.latency = latency;
            this.error = error;
        }
    }
}
