# AI-RPC Framework

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)
[![Python](https://img.shields.io/badge/Python-3.9+-green.svg)](https://python.org/)

A high-performance RPC framework with **AI-powered predictive load balancing**.

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
| `rpc-core` | Core RPC implementation (client, server, codec, loadbalancer) |
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
- Prometheus (for AI load balancing)

### 1. Start Nacos

```bash
docker run -d --name nacos -p 8848:8848 -e MODE=standalone nacos/nacos-server:v2.1.0
```

### 2. Start Prometheus (Optional, for AI load balancing)

```bash
docker run -d --name prometheus -p 9090:9090 prom/prometheus
```

### 3. Start AI Forecasting Service

```bash
cd ai-forecasting-service
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 4. Build Java Project

```bash
mvn clean install
```

### 5. Start Provider

```bash
java -jar example-provider/target/example-provider-1.0-SNAPSHOT.jar
```

### 6. Start Consumer

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

### Configuration

```properties
# rpc.properties
rpc.loadbalancer.type=ai
rpc.loadbalancer.ai.service.url=http://localhost:8000/predict
```

## 📊 Protocol Format

```
┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐
│ Magic   │ Version │Serialize│ MsgType │ MsgId   │ Length  │ Data    │
│ 4 bytes │ 1 byte  │ 1 byte  │ 1 byte  │ 4 bytes │ 4 bytes │ N bytes │
└─────────┴─────────┴─────────┴─────────┴─────────┴─────────┴─────────┘
```

## 🔧 Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `rpc.loadbalancer.type` | `random` | Load balancer type: `random`, `ai` |
| `rpc.loadbalancer.ai.service.url` | `http://localhost:8000/predict` | AI service endpoint |
| `rpc.client.request.timeout-seconds` | `10` | RPC request timeout |

## 📈 Roadmap

- [ ] Spring Boot Starter
- [ ] Annotation-driven configuration (@AiRpcService, @AiRpcReference)
- [ ] Circuit breaker (Resilience4j integration)
- [ ] Multiple registry support (Zookeeper, Consul)
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Rate limiting
- [ ] Gray release / Traffic coloring

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Contact

- Author: raftwang
- Email: raftwang@tencent.com
