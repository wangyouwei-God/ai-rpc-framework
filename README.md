# AI-RPC Framework

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![Python](https://img.shields.io/badge/Python-3.9+-green.svg)](https://python.org/)

**AI-Native RPC Framework** — The first RPC framework with AI-powered intelligent service governance.

## 🚀 Features

- **AI-Driven Load Balancing**: Uses machine learning to predict server health and route traffic intelligently
- **High Performance**: Built on Netty for async, non-blocking I/O
- **Service Discovery**: Nacos integration for dynamic service registration
- **Connection Pooling**: Efficient connection management with Netty FixedChannelPool
- **Custom Protocol**: Binary protocol with magic number validation
- **Multiple Serializers**: Protostuff, JDK serialization support
- **SSL/TLS**: Secure communication out of the box
- **Graceful Shutdown**: Zero-downtime deployment support
- **Metrics**: Micrometer + Prometheus integration

## 📋 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                       AI-RPC FRAMEWORK                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Consumer                                                           │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ RpcProxy → AIPredictiveLoadBalancer → ChannelPoolManager    │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                            │                                         │
│                            ▼                                         │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │               Nacos Registry (Service Discovery)             │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                            │                                         │
│                            ▼                                         │
│   Provider                                                           │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ RpcServer (Netty) → RpcServerHandler → Service Impl          │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│   AI Service (Python)                                                │
│   ┌─────────────────────────────────────────────────────────────┐   │
│   │ FastAPI → Prometheus Query → Exponential Decay Score         │   │
│   └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 🛠️ Modules

| Module | Description |
|--------|-------------|
| `rpc-api` | Service interface definitions |
| `rpc-core` | Core RPC implementation |
| `rpc-registry` | Service registry abstraction |
| `example-provider` | Example RPC service provider |
| `example-consumer` | Example RPC service consumer |
| `ai-forecasting-service` | Python AI prediction service |

## 🚀 Quick Start

### Prerequisites

- Java 11+
- Maven 3.6+
- Python 3.9+
- Nacos Server
- Prometheus (optional, for AI load balancing)

### 1. Start Nacos

```bash
docker run -d --name nacos -p 8848:8848 -e MODE=standalone nacos/nacos-server:v2.1.0
```

### 2. Start AI Forecasting Service

```bash
cd ai-forecasting-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 3. Build Java Project

```bash
mvn clean install
```

### 4. Start Provider

```bash
java -jar example-provider/target/example-provider-1.0-SNAPSHOT.jar
```

### 5. Start Consumer

```bash
java -jar example-consumer/target/example-consumer-1.0-SNAPSHOT.jar
```

## 🧠 AI Predictive Load Balancer

The core innovation of this framework is AI-powered load balancing.

### How It Works

1. **Background Thread**: Every 10 seconds, queries AI service for node health scores
2. **Prometheus Metrics**: AI service queries response latency from Prometheus
3. **Exponential Decay**: `health_score = e^(-k * latency)` — lower latency = higher score
4. **Weighted Random**: Nodes with higher scores receive more traffic
5. **Automatic Fallback**: If AI service fails, degrades to random selection

## 📈 Roadmap

- [ ] Spring Boot Starter
- [ ] Annotation-driven configuration
- [ ] Circuit breaker (Resilience4j)
- [ ] Multiple registry support (Zookeeper, Consul)
- [ ] Distributed tracing (OpenTelemetry)

## 📄 License

Apache License 2.0

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
