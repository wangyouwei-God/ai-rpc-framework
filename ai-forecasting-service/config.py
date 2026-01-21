"""
Configuration for AI Forecasting Service
"""
import os

# Prometheus Configuration
PROMETHEUS_URL = os.getenv("PROMETHEUS_URL", "http://localhost:9090")

# Model Configuration
MODEL_RETRAIN_INTERVAL_HOURS = int(os.getenv("MODEL_RETRAIN_INTERVAL_HOURS", "6"))
MIN_DATA_POINTS_FOR_PROPHET = int(os.getenv("MIN_DATA_POINTS", "60"))  # 1 hour of 1-min data
PREDICTION_HORIZON_MINUTES = int(os.getenv("PREDICTION_HORIZON", "5"))

# Weight Configuration for Multi-Dimensional Scoring
WEIGHTS = {
    "current_latency": 0.35,
    "error_rate": 0.25,
    "predicted_trend": 0.25,
    "anomaly_score": 0.15
}

# Prophet Configuration
PROPHET_CONFIG = {
    "changepoint_prior_scale": 0.05,
    "seasonality_mode": "multiplicative",
    "daily_seasonality": True,
    "weekly_seasonality": True,
    "uncertainty_samples": 500
}

# Anomaly Detection
ANOMALY_CONTAMINATION = 0.1  # Expected % of anomalies

# Caching
CACHE_TTL_SECONDS = 10
