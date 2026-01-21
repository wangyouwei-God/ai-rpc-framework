# Roadmap

This document outlines the development roadmap for AI-RPC Framework.

## Phase 1: Foundation (v1.0)

**Status: In Progress**

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

### In Progress
- [ ] Unit test coverage > 70%
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Documentation

---

## Phase 2: Spring Integration (v1.1)

**Status: Planned**

Spring Boot starter with annotation-driven configuration.

### Features
- [ ] `ai-rpc-spring-boot-starter` module
- [ ] `@AiRpcService` annotation for providers
- [ ] `@AiRpcReference` annotation for consumers
- [ ] YAML configuration support
- [ ] Auto-configuration
- [ ] Actuator integration

### Expected API

```java
// Provider
@AiRpcService
public class HelloServiceImpl implements HelloService {
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}

// Consumer
@Service
public class ConsumerService {
    @AiRpcReference
    private HelloService helloService;
}
```

---

## Phase 3: AI Enhancement (v1.2)

**Status: Planned**

Advanced AI-powered service governance.

### Features
- [ ] Multi-dimensional health scoring (latency, CPU, memory, error rate)
- [ ] AI-CircuitBreaker with anomaly detection
- [ ] AI-Timeout with P99-based self-tuning
- [ ] AI-Retry with intelligent decision making
- [ ] Pluggable ML model support (SPI)

### AI-CircuitBreaker Design

```
Traditional: if (failures > 10) { open() }

AI-Powered:
1. Calculate baseline from historical data
2. Detect anomalies using statistical methods
3. Predictive opening based on error trend
4. Adaptive recovery with gradual traffic increase
```

---

## Phase 4: Ecosystem (v1.3)

**Status: Planned**

Extended registry and observability support.

### Features
- [ ] Zookeeper registry
- [ ] Consul registry
- [ ] etcd registry
- [ ] OpenTelemetry distributed tracing
- [ ] Prometheus metrics endpoint
- [ ] Grafana dashboard templates

---

## Phase 5: Production Ready (v2.0)

**Status: Planned**

Enterprise features and stability.

### Features
- [ ] Rate limiting
- [ ] Gray release / Traffic coloring
- [ ] Service mesh integration
- [ ] Multi-language clients (Go, Python)
- [ ] Performance optimization
- [ ] Production deployment guide

---

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for how to participate in development.

## Feedback

Feature requests and suggestions are welcome. Please open an issue on GitHub.
