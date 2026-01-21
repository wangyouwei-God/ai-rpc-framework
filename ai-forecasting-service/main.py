"""
AI Forecasting Service for RPC Load Balancing v3.0

Features:
- Prophet-based time series forecasting
- Multi-dimensional health scoring
- Anomaly detection with Isolation Forest
- Progressive learning with confidence estimation
"""
from fastapi import FastAPI, BackgroundTasks
from typing import Dict, List, Optional
from datetime import datetime
import asyncio
import logging
import math

from config import (
    PROMETHEUS_URL, 
    MODEL_RETRAIN_INTERVAL_HOURS,
    MIN_DATA_POINTS_FOR_PROPHET
)
from collectors.prometheus import PrometheusCollector
from models.prophet_model import ProphetModelRegistry
from models.anomaly import AnomalyDetectorRegistry
from models.health_scorer import HealthScorer, NodeHealth

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="AI Forecasting Service for RPC Load Balancing",
    description="""
    Advanced AI-powered load balancing predictions using:
    - Prophet for time series forecasting
    - Isolation Forest for anomaly detection
    - Multi-dimensional health scoring
    """,
    version="3.0.0"
)

# Initialize components
prometheus = PrometheusCollector(PROMETHEUS_URL)
prophet_registry = ProphetModelRegistry()
anomaly_registry = AnomalyDetectorRegistry()
health_scorer = HealthScorer()


# ============== API Endpoints ==============

@app.post("/predict")
async def predict_health_scores(nodes: List[str], background_tasks: BackgroundTasks):
    """
    Predict health scores for a list of nodes.
    
    Returns simple {node: score} format for Java client backward compatibility.
    Use /predict/detailed for full diagnostics.
    """
    detailed = await _get_detailed_predictions(nodes, background_tasks)
    
    # Return simple {node: score} format for Java client
    simple_results = {}
    for node, data in detailed.items():
        if isinstance(data, dict):
            simple_results[node] = round(data.get("health_score", 0.5), 4)
        else:
            simple_results[node] = round(data, 4)
    
    logger.info(f"Prediction results: {simple_results}")
    return simple_results


@app.post("/predict/detailed")
async def predict_health_scores_detailed(nodes: List[str], background_tasks: BackgroundTasks):
    """
    Predict health scores with full diagnostics.
    Returns detailed information including confidence, trend, anomaly detection.
    """
    return await _get_detailed_predictions(nodes, background_tasks)


async def _get_detailed_predictions(nodes: List[str], background_tasks: BackgroundTasks):
    """Internal: Get detailed predictions for nodes."""
    logger.info(f"Prediction request for nodes: {nodes}")
    
    # Check Prometheus availability
    prometheus_available = prometheus.is_available()
    
    if not prometheus_available:
        logger.warning("Prometheus unavailable, returning default scores")
        return {node: {"health_score": 0.5, "confidence": 0.0} for node in nodes}
    
    # Collect current metrics
    current_latencies = prometheus.get_current_latency(nodes)
    current_errors = prometheus.get_error_rate(nodes)
    
    results = {}
    nodes_needing_training = []
    
    for node in nodes:
        current_latency = current_latencies.get(node, 0)
        error_rate = current_errors.get(node, 0)
        
        # Get or create Prophet model
        prophet_model = prophet_registry.get_or_create(node)
        
        # Get or create anomaly detector
        anomaly_detector = anomaly_registry.get_or_create(node)
        
        # Update anomaly detector with current observation
        anomaly_detector.add_observation(current_latency, error_rate)
        
        # Check for anomaly
        is_anomaly, anomaly_score = anomaly_detector.is_anomaly(current_latency, error_rate)
        
        # Get prediction if model is trained
        if prophet_model.is_trained():
            predicted_latency, confidence = prophet_model.predict()
            trend = prophet_model.get_trend()
        else:
            predicted_latency = current_latency
            confidence = 0.0
            trend = "unknown"
            
            # Schedule training if needed
            if prophet_model.needs_retrain():
                nodes_needing_training.append(node)
        
        # Calculate comprehensive health score
        if confidence > 0.2:
            # Use full multi-dimensional scoring
            node_health = health_scorer.calculate_health(
                node=node,
                current_latency=current_latency,
                predicted_latency=predicted_latency,
                error_rate=error_rate,
                is_anomaly=is_anomaly,
                anomaly_score=anomaly_score,
                trend=trend,
                prediction_confidence=confidence
            )
            results[node] = node_health.to_dict()
        else:
            # Fallback to simple exponential decay
            simple_score = health_scorer.calculate_simple_score(current_latency)
            
            # Still apply error rate penalty
            if error_rate > 0.1:
                simple_score *= (1 - error_rate)
            
            results[node] = {
                "health_score": round(simple_score, 4),
                "current_latency": round(current_latency, 6),
                "predicted_latency": round(current_latency, 6),
                "error_rate": round(error_rate, 4),
                "is_anomaly": is_anomaly,
                "anomaly_score": round(anomaly_score, 4),
                "trend": "unknown",
                "confidence": 0.0
            }
    
    # Schedule background training for nodes that need it
    if nodes_needing_training:
        background_tasks.add_task(train_models, nodes_needing_training)
    
    return results


@app.post("/train")
async def train_model(node: str, history_hours: int = 24):
    """
    Manually trigger model training for a specific node.
    """
    logger.info(f"Manual training request for {node}, history={history_hours}h")
    
    # Fetch historical data
    df = prometheus.get_historical_latency(node, hours=history_hours)
    
    if df is None or len(df) < MIN_DATA_POINTS_FOR_PROPHET:
        return {
            "status": "insufficient_data",
            "data_points": len(df) if df is not None else 0,
            "required": MIN_DATA_POINTS_FOR_PROPHET
        }
    
    # Train model
    prophet_model = prophet_registry.get_or_create(node)
    success = prophet_model.train(df)
    
    if success:
        return {
            "status": "trained",
            "data_points": len(df),
            "mape": round(prophet_model.last_mape, 4)
        }
    else:
        return {"status": "failed"}


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    prometheus_ok = prometheus.is_available()
    
    return {
        "status": "ok" if prometheus_ok else "degraded",
        "prometheus_connected": prometheus_ok,
        "models_loaded": len(prophet_registry.list_nodes()),
        "version": "3.0.0"
    }


@app.get("/models")
async def list_models():
    """List all trained models and their status."""
    models_info = {}
    
    for node in prophet_registry.list_nodes():
        model = prophet_registry.get(node)
        if model:
            models_info[node] = {
                "is_trained": model.is_trained(),
                "data_points": model.data_points_count,
                "last_trained": model.last_trained.isoformat() if model.last_trained else None,
                "mape": round(model.last_mape, 4) if model.is_trained() else None,
                "needs_retrain": model.needs_retrain()
            }
    
    return models_info


# ============== Background Tasks ==============

async def train_models(nodes: List[str]):
    """Background task to train models for multiple nodes."""
    for node in nodes:
        try:
            logger.info(f"Background training for {node}")
            df = prometheus.get_historical_latency(node, hours=24)
            
            if df is not None and len(df) >= MIN_DATA_POINTS_FOR_PROPHET:
                prophet_model = prophet_registry.get_or_create(node)
                prophet_model.train(df)
        except Exception as e:
            logger.error(f"Background training failed for {node}: {e}")
        
        # Small delay between trainings
        await asyncio.sleep(1)
# ============== Startup/Shutdown ==============

@app.on_event("startup")
async def startup_event():
    logger.info("AI Forecasting Service v3.0 starting...")
    logger.info(f"Prometheus URL: {PROMETHEUS_URL}")
    
    if prometheus.is_available():
        logger.info("Prometheus connection: OK")
    else:
        logger.warning("Prometheus connection: FAILED - running in degraded mode")


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("AI Forecasting Service shutting down...")
