# AI-RPC Framework Design Vision

## Core Positioning

**"AI-Native RPC Framework"** — The first RPC framework with AI deeply integrated into the core service governance layer.

### Differentiation

| Framework | Focus | Core Strength |
|-----------|-------|---------------|
| Dubbo | Enterprise Service Governance | Complete ecosystem, Alibaba backing |
| gRPC | High-performance Cross-language | Google backing, HTTP/2 |
| Thrift | Cross-language Serialization | Facebook backing |
| **AI-RPC** | **AI-Native Intelligent RPC** | **AI-driven adaptive governance** |

---

## Design Philosophy

### Traditional RPC vs AI-RPC

**Traditional RPC (Configuration-Driven)**

```
Load Balancing: Round-Robin / Random / Static Weights
Circuit Breaker: 10 failures trigger open
Timeout: 3000ms fixed
Retry: 3 times fixed

Problems:
- Parameters require manual tuning
- Cannot adapt to runtime changes
- Different services need different configurations
```

**AI-RPC (AI-Driven)**

```
Load Balancing: Real-time health prediction, dynamic weights
Circuit Breaker: Adaptive threshold based on historical patterns
Timeout: Self-tuning based on P99 latency
Retry: Intelligent decision based on error classification

Advantages:
- Zero-configuration self-adaptation
- Continuous runtime optimization
- One framework fits all services
```

---

## Architecture

```
+-----------------------------------------------------------------------+
|                         AI-RPC FRAMEWORK                               |
+-----------------------------------------------------------------------+
|                                                                        |
|   Application Layer                                                    |
|   +---------------------------------------------------------------+   |
|   | @AiRpcService  @AiRpcReference  Spring Boot Starter           |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
|   Governance Layer                                                     |
|   +---------------+---------------+---------------+---------------+   |
|   | AI-LoadBalance| AI-CircuitBrk | AI-Timeout    | AI-Retry      |   |
|   | Predictive    | Adaptive      | Self-tuning   | Smart         |   |
|   | Routing       | Threshold     | Deadline      | Decision      |   |
|   +---------------+---------------+---------------+---------------+   |
|                                                                        |
|   AI Brain Layer                                                       |
|   +---------------------------------------------------------------+   |
|   | Metrics Collector -> Feature Engineering -> ML Models          |   |
|   | - Latency histogram   - Sliding window      - LightGBM        |   |
|   | - Error rate          - Trend detection     - Exponential     |   |
|   | - CPU/Memory          - Anomaly score       - Neural Network  |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
|   Transport Layer                                                      |
|   +---------------------------------------------------------------+   |
|   | Netty | Custom Protocol | Protostuff | Connection Pool        |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
|   Registry Layer                                                       |
|   +---------------------------------------------------------------+   |
|   | Nacos | Zookeeper | Consul | etcd                             |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
+-----------------------------------------------------------------------+
```

---

## AI Brain Modules

### 1. AI-LoadBalancer

**Current**: Single latency metric with exponential decay
**Target**: Multi-dimensional metrics with pluggable models

Input Features:
- latency_p50, latency_p95, latency_p99
- error_rate_1m, error_rate_5m
- cpu_usage, memory_usage
- active_connections, queue_length
- gc_pause_time, gc_frequency

Model Options (SPI extensible):
- ExponentialDecay (lightweight, default)
- LightGBM (accurate, requires training)
- NeuralNetwork (complex scenarios)
- Custom (user-defined)

Output:
```
health_scores = {node_a: 0.85, node_b: 0.62, node_c: 0.23}
```

### 2. AI-CircuitBreaker

**Traditional**: Fixed threshold (e.g., 10 failures trigger)
**AI-Powered**: Adaptive threshold with predictive opening

Algorithm:
1. Calculate baseline from historical data
2. Detect anomalies using statistical methods (3-sigma)
3. Predictive opening based on error rate trend
4. Adaptive recovery with gradual traffic increase

### 3. AI-Timeout

**Traditional**: Fixed configuration (e.g., 3000ms)
**AI-Powered**: Self-tuning based on P99 latency

Algorithm:
```
latency_histogram = collect(last_5_minutes)
p99 = latency_histogram.percentile(99)
adaptive_timeout = p99 * 1.5
timeout = clamp(adaptive_timeout, min=100ms, max=30s)
```

Benefits:
- Fast services get short timeouts (avoid resource waste)
- Slow services get longer timeouts (avoid false positives)
- No manual tuning required

### 4. AI-Retry

**Traditional**: Fixed retry count for all errors
**AI-Powered**: Intelligent decision based on error classification

Error Classification:
- Retryable: TIMEOUT, CONNECTION_REFUSED, SERVICE_UNAVAILABLE
- Non-Retryable: BUSINESS_ERROR, INVALID_ARGUMENT, NOT_FOUND

Decision Model:
```
retry_probability = model.predict(
    error_type,
    node_health_score,
    historical_retry_success_rate,
    current_system_load
)

if (retry_probability > 0.5) {
    retry_with_different_node()
} else {
    fail_fast()
}
```

---

## Module Structure

```
ai-rpc-framework/
├── ai-rpc-api/                    # Core API definitions
├── ai-rpc-core/                   # Core implementation
│   ├── transport/                 # Network transport
│   ├── codec/                     # Encoding/Decoding
│   ├── loadbalance/               # Load balancing
│   ├── circuitbreaker/            # Circuit breaker
│   ├── retry/                     # Retry strategy
│   └── timeout/                   # Timeout strategy
│
├── ai-rpc-brain/                  # AI Brain (new)
│   ├── collector/                 # Metrics collection
│   ├── features/                  # Feature engineering
│   ├── models/                    # ML models
│   │   ├── exponential/           # Exponential decay
│   │   ├── lightgbm/              # LightGBM
│   │   └── neural/                # Neural network
│   └── prediction/                # Prediction service
│
├── ai-rpc-registry/               # Registry implementations
│   ├── nacos/
│   ├── zookeeper/
│   └── consul/
│
├── ai-rpc-spring-boot-starter/    # Spring integration (new)
│
├── ai-rpc-tracing/                # Distributed tracing (new)
│
├── ai-forecasting-service/        # Python AI service
│
└── examples/
    ├── quickstart/
    ├── spring-boot-example/
    └── ai-features-demo/
```

---

## Roadmap

### Phase 1: Foundation (v1.0) - 2 weeks

- [x] Core RPC functionality
- [x] Basic AI load balancing
- [x] Nacos registry
- [x] README, LICENSE, CONTRIBUTING
- [x] Fix hardcoded weights bug
- [x] GitHub Actions CI
- [ ] Unit tests > 70% coverage
- [ ] Integration tests

### Phase 2: Spring Integration (v1.1) - 2 weeks

- [ ] Spring Boot Starter
- [ ] @AiRpcService annotation
- [ ] @AiRpcReference annotation
- [ ] YAML configuration
- [ ] Auto-configuration

### Phase 3: AI Enhancement (v1.2) - 3 weeks

- [ ] Multi-dimensional metrics collection
- [ ] AI-CircuitBreaker
- [ ] AI-Timeout
- [ ] AI-Retry
- [ ] Model SPI

### Phase 4: Ecosystem (v1.3) - 2 weeks

- [ ] Zookeeper registry
- [ ] Consul registry
- [ ] OpenTelemetry tracing
- [ ] Prometheus metrics export
- [ ] Grafana dashboard templates

### Phase 5: Production Ready (v2.0)

- [ ] Rate limiting
- [ ] Gray release
- [ ] Service mesh integration
- [ ] Multi-language clients
- [ ] Performance benchmarks

---

## Key Selling Points

1. **AI-Native**: AI is integrated from the design phase, not an afterthought
2. **Zero-Configuration**: System self-optimizes without manual tuning
3. **Explainable AI**: Every decision has a clear reason for debugging
4. **Progressive Adoption**: Use traditional features or enable AI features
5. **Production-Proven**: Based on experience from Tencent engineering

---

## Target Users

### Primary Users
- Medium to large enterprises with microservices architecture
- Financial and e-commerce companies requiring high stability
- Teams wanting to reduce operations configuration work

### Use Cases
- Service-to-service RPC calls
- Traffic governance
- Automated fault recovery
- Performance optimization

---

Document Version: 1.0
Last Updated: 2026-01-21
