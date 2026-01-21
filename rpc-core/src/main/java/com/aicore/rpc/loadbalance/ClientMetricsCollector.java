package com.aicore.rpc.loadbalance;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aicore.rpc.circuitbreaker.CircuitBreaker;
import com.aicore.rpc.circuitbreaker.CircuitBreakerRegistry;
import com.aicore.rpc.circuitbreaker.CircuitBreakerState;
import com.aicore.rpc.timeout.AdaptiveTimeout;
import com.aicore.rpc.timeout.AdaptiveTimeoutRegistry;
import com.aicore.rpc.timeout.LatencyStatistics;

/**
 * Collects client-side metrics for load balancing decisions.
 * Aggregates data from circuit breakers and adaptive timeouts.
 */
public class ClientMetricsCollector {

    private static final ClientMetricsCollector INSTANCE = new ClientMetricsCollector();

    public static ClientMetricsCollector getInstance() {
        return INSTANCE;
    }

    /**
     * Collect metrics for all known endpoints of a service.
     * 
     * @param serviceName the service name
     * @param addresses   available addresses
     * @return metrics for each address
     */
    public Map<InetSocketAddress, EndpointMetrics> collectMetrics(
            String serviceName, java.util.List<InetSocketAddress> addresses) {

        Map<InetSocketAddress, EndpointMetrics> metrics = new ConcurrentHashMap<>();

        for (InetSocketAddress address : addresses) {
            String key = serviceName + "@" + address.getHostString() + ":" + address.getPort();
            EndpointMetrics em = collectEndpointMetrics(key, address);
            metrics.put(address, em);
        }

        return metrics;
    }

    /**
     * Collect metrics for a single endpoint.
     */
    private EndpointMetrics collectEndpointMetrics(String key, InetSocketAddress address) {
        EndpointMetrics em = new EndpointMetrics();
        em.setAddress(address);

        // Get circuit breaker metrics
        CircuitBreaker cb = CircuitBreakerRegistry.getInstance().get(key);
        if (cb != null) {
            em.setCircuitBreakerState(cb.getState());
            em.setFailureRate(cb.getMetrics().getFailureRate());
            em.setSlowCallRate(cb.getMetrics().getSlowCallRate());
            em.setTotalCalls(cb.getMetrics().getTotalCalls());
        } else {
            em.setCircuitBreakerState(CircuitBreakerState.CLOSED);
            em.setFailureRate(0);
            em.setSlowCallRate(0);
            em.setTotalCalls(0);
        }

        // Get adaptive timeout metrics
        AdaptiveTimeout at = AdaptiveTimeoutRegistry.getInstance().get(key);
        if (at != null) {
            LatencyStatistics stats = at.getStatistics();
            em.setP50Latency(stats.getP50());
            em.setP99Latency(stats.getP99());
            em.setAvgLatency(stats.getAverage());
            em.setSampleCount(stats.getSampleCount());
        }

        return em;
    }

    /**
     * Calculate local weight adjustment based on client metrics.
     * Used for fast local decisions without waiting for AI service.
     * 
     * @param metrics endpoint metrics
     * @return weight multiplier (0.0 to 1.0)
     */
    public double calculateLocalWeight(EndpointMetrics metrics) {
        double weight = 1.0;

        // Circuit breaker state adjustment
        switch (metrics.getCircuitBreakerState()) {
            case OPEN:
                return 0.0; // Exclude open circuits
            case HALF_OPEN:
                weight *= 0.3; // Reduce weight for half-open
                break;
            case CLOSED:
                break;
        }

        // Failure rate adjustment
        if (metrics.getFailureRate() > 50) {
            weight *= 0.2;
        } else if (metrics.getFailureRate() > 20) {
            weight *= 0.5;
        } else if (metrics.getFailureRate() > 10) {
            weight *= 0.8;
        }

        // Slow call rate adjustment
        if (metrics.getSlowCallRate() > 50) {
            weight *= 0.5;
        } else if (metrics.getSlowCallRate() > 20) {
            weight *= 0.8;
        }

        return weight;
    }

    /**
     * Metrics for a single endpoint.
     */
    public static class EndpointMetrics {
        private InetSocketAddress address;
        private CircuitBreakerState circuitBreakerState;
        private float failureRate;
        private float slowCallRate;
        private int totalCalls;
        private long p50Latency;
        private long p99Latency;
        private double avgLatency;
        private int sampleCount;

        // Getters and setters
        public InetSocketAddress getAddress() {
            return address;
        }

        public void setAddress(InetSocketAddress address) {
            this.address = address;
        }

        public CircuitBreakerState getCircuitBreakerState() {
            return circuitBreakerState;
        }

        public void setCircuitBreakerState(CircuitBreakerState state) {
            this.circuitBreakerState = state;
        }

        public float getFailureRate() {
            return failureRate;
        }

        public void setFailureRate(float failureRate) {
            this.failureRate = failureRate;
        }

        public float getSlowCallRate() {
            return slowCallRate;
        }

        public void setSlowCallRate(float slowCallRate) {
            this.slowCallRate = slowCallRate;
        }

        public int getTotalCalls() {
            return totalCalls;
        }

        public void setTotalCalls(int totalCalls) {
            this.totalCalls = totalCalls;
        }

        public long getP50Latency() {
            return p50Latency;
        }

        public void setP50Latency(long p50Latency) {
            this.p50Latency = p50Latency;
        }

        public long getP99Latency() {
            return p99Latency;
        }

        public void setP99Latency(long p99Latency) {
            this.p99Latency = p99Latency;
        }

        public double getAvgLatency() {
            return avgLatency;
        }

        public void setAvgLatency(double avgLatency) {
            this.avgLatency = avgLatency;
        }

        public int getSampleCount() {
            return sampleCount;
        }

        public void setSampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("address", address.getHostString() + ":" + address.getPort());
            map.put("circuitBreakerState", circuitBreakerState.name());
            map.put("failureRate", failureRate);
            map.put("slowCallRate", slowCallRate);
            map.put("totalCalls", totalCalls);
            map.put("p50Latency", p50Latency);
            map.put("p99Latency", p99Latency);
            map.put("avgLatency", avgLatency);
            map.put("sampleCount", sampleCount);
            return map;
        }
    }
}
