# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2026-01-21

### Added
- **Prophet-based AI Forecasting** - Time series prediction using Facebook Prophet
- **Anomaly Detection** - Isolation Forest based anomaly detection for latency patterns
- **Multi-dimensional Health Scoring** - Combines latency, error rate, prediction trend, and anomaly score
- **Progressive Learning** - Confidence increases as more data is collected
- **Detailed Prediction API** - `/predict/detailed` endpoint with full diagnostics

### Changed
- AI Forecasting Service upgraded to v3.0
- Health score now incorporates trend prediction and anomaly detection
- OkHttpClient configured with proper timeouts (3s connect, 5s read)

### Fixed
- Thread safety issue in AIPredictiveLoadBalancer (defensive copy)
- OkHttpClient missing timeout configuration
- rpc.properties UTF-8 encoding for Chinese comments
- Added graceful shutdown hook for scheduler and connection pool

---

## [2.0.0] - 2026-01-20

### Added
- Circuit Breaker with CLOSED/OPEN/HALF_OPEN state machine
- Adaptive Timeout based on P99 latency statistics
- Smart Retry with exponential backoff and jitter
- Client-side metrics fusion for load balancing
- 112 unit and integration tests

### Changed
- Load balancer now combines AI weights with local metrics

---

## [1.0.0] - 2026-01-19

### Added
- AI-powered predictive load balancing
- Nacos service discovery integration
- Connection pooling with Netty FixedChannelPool
- Custom binary protocol with magic number validation
- Protostuff and JDK serialization support
- SSL/TLS encryption
- Graceful shutdown support
- Micrometer metrics integration
- Python-based AI forecasting service
- Spring Boot Starter with annotations

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 3.0.0 | 2026-01-21 | Prophet AI prediction, anomaly detection |
| 2.0.0 | 2026-01-20 | Circuit breaker, adaptive timeout, retry |
| 1.0.0 | 2026-01-19 | Initial release |
