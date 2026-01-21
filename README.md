# AI-RPC Framework

[![Build Status](https://github.com/wangyouwei-God/ai-rpc-framework/actions/workflows/ci.yml/badge.svg)](https://github.com/wangyouwei-God/ai-rpc-framework/actions)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://openjdk.java.net/)

AI-RPC is a high-performance RPC framework with AI-powered intelligent service governance. Unlike traditional RPC frameworks that rely on static configuration, AI-RPC uses machine learning to dynamically optimize load balancing, timeout, and circuit breaking decisions.

## Overview

Traditional RPC frameworks require manual tuning of parameters like load balancing weights, timeout values, and circuit breaker thresholds. AI-RPC introduces an AI Brain layer that:

- Predicts server health based on real-time metrics
- Automatically routes traffic to optimal nodes
- Self-tunes timeout values based on P99 latency
- Detects anomalies before failures occur

## Features

| Feature | Description |
|---------|-------------|
| **AI Load Balancing** | Predictive routing based on server health scores |
| **High Performance** | Netty-based async I/O with connection pooling |
| **Service Discovery** | Nacos integration with health checking |
| **Custom Protocol** | Binary protocol with magic number validation |
| **Multiple Serializers** | Protostuff, JDK serialization |
| **SSL/TLS** | Secure communication by default |
| **Graceful Shutdown** | Zero-downtime deployment support |
| **Observability** | Micrometer metrics with Prometheus export |

## Architecture

```
+-----------------------------------------------------------------------+
|                         AI-RPC FRAMEWORK                               |
+-----------------------------------------------------------------------+
|                                                                        |
|   +---------------------------+    +---------------------------+       |
|   |       Consumer            |    |       Provider            |       |
|   +---------------------------+    +---------------------------+       |
|   | RpcProxy                  |    | RpcServer                 |       |
|   | AIPredictiveLoadBalancer  |    | RpcServerHandler          |       |
|   | ChannelPoolManager        |    | ServiceRegistry           |       |
|   +-------------+-------------+    +-------------+-------------+       |
|                 |                                |                     |
|                 v                                v                     |
|   +---------------------------------------------------------------+   |
|   |                    Nacos Registry                              |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
|   +---------------------------------------------------------------+   |
|   |                    AI Forecasting Service                      |   |
|   |   Prometheus Query -> Feature Engineering -> Health Score      |   |
|   +---------------------------------------------------------------+   |
|                                                                        |
+-----------------------------------------------------------------------+
```

## Modules

| Module | Description |
|--------|-------------|
| `rpc-api` | Service interface definitions |
| `rpc-core` | Core RPC client and server implementation |
| `rpc-registry` | Service registry abstraction layer |
| `example-provider` | Example service provider |
| `example-consumer` | Example service consumer |
| `ai-forecasting-service` | Python-based AI prediction service |

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher
- Python 3.9 or higher (for AI service)
- Nacos Server 2.x
- Prometheus (optional, for AI load balancing)

### 1. Start Infrastructure

```bash
# Start Nacos
docker run -d --name nacos \
    -p 8848:8848 \
    -e MODE=standalone \
    nacos/nacos-server:v2.1.0

# Start Prometheus (optional)
docker run -d --name prometheus \
    -p 9090:9090 \
    prom/prometheus
```

### 2. Start AI Forecasting Service

```bash
cd ai-forecasting-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

### 3. Build and Run

```bash
# Build
mvn clean install -DskipTests

# Start Provider
java -jar example-provider/target/example-provider-1.0-SNAPSHOT.jar

# Start Consumer
java -jar example-consumer/target/example-consumer-1.0-SNAPSHOT.jar
```

## Configuration

Configuration is loaded from `rpc.properties` in classpath:

```properties
# Load Balancer
rpc.loadbalancer.type=ai
rpc.loadbalancer.ai.service.url=http://localhost:8000/predict

# Client
rpc.client.request.timeout-seconds=10

# Registry
rpc.registry.address=127.0.0.1:8848
```

## AI Load Balancing

The AI Load Balancer works by:

1. A background thread queries the AI service every 10 seconds
2. The AI service collects metrics from Prometheus (latency, error rate, etc.)
3. Health scores are calculated using exponential decay: `score = e^(-k * latency)`
4. Weighted random selection routes traffic to healthier nodes
5. If AI service is unavailable, automatic fallback to random selection

### Health Score Calculation

| Latency (ms) | Health Score |
|--------------|--------------|
| 10 | 0.82 |
| 50 | 0.37 |
| 100 | 0.14 |
| 200 | 0.02 |

## Protocol Format

```
+--------+--------+----------+---------+---------+--------+------+
| Magic  |Version |Serializer|Msg Type | Msg ID  | Length | Data |
| 4bytes | 1byte  | 1byte    | 1byte   | 4bytes  | 4bytes | N    |
+--------+--------+----------+---------+---------+--------+------+
```

## Roadmap

See [ROADMAP.md](docs/ROADMAP.md) for planned features.

### Planned Features

- Spring Boot Starter with annotation support
- Adaptive circuit breaker with anomaly detection
- Self-tuning timeout based on P99 latency
- Multi-registry support (Zookeeper, Consul, etcd)
- OpenTelemetry distributed tracing

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

## License

AI-RPC is licensed under the [Apache License 2.0](LICENSE).

## Acknowledgements

This project is inspired by [Apache Dubbo](https://dubbo.apache.org/) and [gRPC](https://grpc.io/).
