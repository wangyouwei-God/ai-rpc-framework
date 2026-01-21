# Roadmap

This document outlines the development roadmap for AI-RPC Framework.

## Phase 1: Foundation (v1.0) ✅ COMPLETED

Core RPC functionality with basic AI load balancing.

### Completed
- [x] Netty-based client and server
- [x] Custom binary protocol
- [x] Protostuff serialization
- [x] Nacos service discovery
- [x] Connection pooling
- [x] Basic AI load balancing (exponential decay)
- [x] SSL/TLS encryption
- [x] Graceful shutdown
- [x] Micrometer metrics
- [x] Unit test coverage > 70% (112 tests)
- [x] Integration tests
- [x] Performance benchmarks
- [x] Documentation

---

## Phase 2: Spring Integration (v1.1) ✅ COMPLETED

Spring Boot starter with annotation-driven configuration.

### Completed
- [x] `ai-rpc-spring-boot-starter` module
- [x] `@AiRpcService` annotation for providers
- [x] `@AiRpcReference` annotation for consumers
- [x] YAML configuration support
- [x] Auto-configuration
- [ ] Actuator integration

---

## Phase 3: AI Enhancement (v3.0) ✅ COMPLETED

Advanced AI-powered service governance.

### Completed
- [x] Multi-dimensional health scoring (latency, error rate, trend, anomaly)
- [x] Prophet-based time series forecasting
- [x] Isolation Forest anomaly detection
- [x] Progressive confidence learning
- [x] Circuit Breaker with CLOSED/OPEN/HALF_OPEN states
- [x] Adaptive Timeout with P99-based self-tuning
- [x] Smart Retry with exponential backoff and jitter

### Remaining
- [ ] Pluggable ML model support (SPI)

---

## Phase 4: Ecosystem (v3.1) 🔄 IN PROGRESS

Extended registry and observability support.

### Completed
- [x] Prometheus metrics endpoint
- [x] Grafana dashboard templates
- [x] Docker Compose one-click deployment

### Remaining
- [ ] Zookeeper registry
- [ ] Consul registry
- [ ] etcd registry
- [ ] OpenTelemetry distributed tracing

---

## Phase 5: Production Ready (v4.0) 📋 PLANNED

Enterprise features and stability.

### Features
- [ ] Rate limiting
- [ ] Gray release / Traffic coloring
- [ ] Service mesh integration
- [ ] Multi-language clients (Go, Python)
- [ ] Performance optimization
- [ ] Production deployment guide

---

## Version Summary

| Version | Status | Key Features |
|---------|--------|--------------|
| v1.0 | ✅ Done | Core RPC, Netty, Nacos |
| v2.0 | ✅ Done | Circuit Breaker, Timeout, Retry |
| v3.0 | ✅ Done | Prophet AI, Anomaly Detection |
| v3.1 | 🔄 In Progress | Grafana, Docker Compose |
| v4.0 | 📋 Planned | Rate Limit, Multi-language |

---

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for how to participate in development.

## Feedback

Feature requests and suggestions are welcome. Please open an issue on GitHub.
