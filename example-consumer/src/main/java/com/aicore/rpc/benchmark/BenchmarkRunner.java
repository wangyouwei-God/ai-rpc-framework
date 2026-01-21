package com.aicore.rpc.benchmark;

import com.aicore.rpc.api.HelloService;
import com.aicore.rpc.loadbalance.LoadBalancer;
import com.aicore.rpc.loadbalance.RandomLoadBalancer;
import com.aicore.rpc.proxy.RpcProxy;
import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.registry.nacos.NacosRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance benchmark for AI-RPC Framework.
 * Measures throughput (requests/second) and latency (P50, P99, avg).
 */
public class BenchmarkRunner {

    private static final String NACOS_ADDRESS = "127.0.0.1:8848";

    public static void main(String[] args) throws Exception {
        System.setProperty("nacos.remote.client.grpc.tls.enable", "false");

        // Parse arguments
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 10;
        int totalRequests = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
        int warmupRequests = args.length > 2 ? Integer.parseInt(args[2]) : 100;

        System.out.println("==============================================");
        System.out.println("      AI-RPC Framework Performance Benchmark");
        System.out.println("==============================================");
        System.out.println("Threads: " + threads);
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Warmup Requests: " + warmupRequests);
        System.out.println();

        // Initialize
        Registry registry = new NacosRegistry(NACOS_ADDRESS);
        LoadBalancer loadBalancer = new RandomLoadBalancer();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        HelloService helloService = RpcProxy.create(HelloService.class, registry, loadBalancer, meterRegistry);

        // Warmup
        System.out.println("Warming up...");
        for (int i = 0; i < warmupRequests; i++) {
            try {
                helloService.sayHello("warmup-" + i);
            } catch (Exception e) {
                // Ignore warmup errors
            }
        }
        System.out.println("Warmup complete.");
        System.out.println();

        // Benchmark
        System.out.println("Running benchmark...");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            final int index = i;
            executor.submit(() -> {
                long callStart = System.nanoTime();
                try {
                    helloService.sayHello("benchmark-" + index);
                    long latency = (System.nanoTime() - callStart) / 1_000_000; // Convert to ms
                    totalLatency.addAndGet(latency);
                    latencies.add(latency);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        // Calculate results
        long duration = endTime - startTime;
        double throughput = (double) successCount.get() / (duration / 1000.0);

        // Sort latencies for percentiles
        Collections.sort(latencies);
        long p50 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.5));
        long p90 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.9));
        long p99 = latencies.isEmpty() ? 0 : latencies.get((int) (latencies.size() * 0.99));
        long minLatency = latencies.isEmpty() ? 0 : latencies.get(0);
        long maxLatency = latencies.isEmpty() ? 0 : latencies.get(latencies.size() - 1);
        double avgLatency = latencies.isEmpty() ? 0 : latencies.stream().mapToLong(l -> l).average().orElse(0);

        // Print results
        System.out.println();
        System.out.println("==============================================");
        System.out.println("              BENCHMARK RESULTS");
        System.out.println("==============================================");
        System.out.println();
        System.out.printf("Duration:        %d ms%n", duration);
        System.out.printf("Total Requests:  %d%n", totalRequests);
        System.out.printf("Success:         %d%n", successCount.get());
        System.out.printf("Failures:        %d%n", failureCount.get());
        System.out.printf("Success Rate:    %.2f%%%n", (successCount.get() * 100.0 / totalRequests));
        System.out.println();
        System.out.println("-- Throughput --");
        System.out.printf("Requests/sec:    %.2f%n", throughput);
        System.out.println();
        System.out.println("-- Latency (ms) --");
        System.out.printf("Min:             %d%n", minLatency);
        System.out.printf("Avg:             %.2f%n", avgLatency);
        System.out.printf("P50 (Median):    %d%n", p50);
        System.out.printf("P90:             %d%n", p90);
        System.out.printf("P99:             %d%n", p99);
        System.out.printf("Max:             %d%n", maxLatency);
        System.out.println();
        System.out.println("==============================================");

        System.exit(0);
    }
}
