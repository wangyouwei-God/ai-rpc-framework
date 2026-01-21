"""
Multi-Dimensional Health Scorer
Combines various metrics to produce a final health score.
"""
import math
from typing import Dict, Optional
from dataclasses import dataclass
import logging

from config import WEIGHTS

logger = logging.getLogger(__name__)


@dataclass
class NodeHealth:
    """Complete health assessment for a node."""
    node: str
    health_score: float
    current_latency: float
    predicted_latency: float
    error_rate: float
    is_anomaly: bool
    anomaly_score: float
    trend: str
    confidence: float
    
    def to_dict(self) -> dict:
        return {
            "health_score": round(self.health_score, 4),
            "current_latency": round(self.current_latency, 6),
            "predicted_latency": round(self.predicted_latency, 6),
            "error_rate": round(self.error_rate, 4),
            "is_anomaly": self.is_anomaly,
            "anomaly_score": round(self.anomaly_score, 4),
            "trend": self.trend,
            "confidence": round(self.confidence, 4)
        }


class HealthScorer:
    """
    Calculates multi-dimensional health scores.
    
    Formula:
        score = w1 * latency_score + 
                w2 * (1 - error_rate) + 
                w3 * trend_score + 
                w4 * (1 - anomaly_score)
    """
    
    def __init__(self, sensitivity_factor: float = 20.0):
        self.sensitivity_factor = sensitivity_factor
        self.weights = WEIGHTS
    
    def calculate_health(
        self,
        node: str,
        current_latency: float,
        predicted_latency: float,
        error_rate: float,
        is_anomaly: bool,
        anomaly_score: float,
        trend: str,
        prediction_confidence: float
    ) -> NodeHealth:
        """
        Calculate comprehensive health score for a node.
        
        Args:
            node: Node identifier
            current_latency: Current average latency in seconds
            predicted_latency: Predicted future latency in seconds
            error_rate: Current error rate (0-1)
            is_anomaly: Whether current state is anomalous
            anomaly_score: Anomaly probability (0-1)
            trend: 'improving', 'stable', 'degrading', 'unknown'
            prediction_confidence: Confidence in the prediction (0-1)
        
        Returns:
            NodeHealth object with complete assessment
        """
        # 1. Current latency score (exponential decay)
        current_latency_score = self._latency_to_score(current_latency)
        
        # 2. Error rate score
        error_rate_score = max(0, 1 - error_rate)
        
        # 3. Predicted trend score
        if prediction_confidence > 0.3:
            predicted_latency_score = self._latency_to_score(predicted_latency)
            # Blend current and predicted based on confidence
            trend_score = (
                (1 - prediction_confidence) * current_latency_score +
                prediction_confidence * predicted_latency_score
            )
        else:
            trend_score = current_latency_score
        
        # 4. Anomaly score (inverted: high anomaly = low score)
        stability_score = max(0, 1 - anomaly_score)
        
        # 5. Calculate weighted final score
        health_score = (
            self.weights["current_latency"] * current_latency_score +
            self.weights["error_rate"] * error_rate_score +
            self.weights["predicted_trend"] * trend_score +
            self.weights["anomaly_score"] * stability_score
        )
        
        # Apply anomaly penalty
        if is_anomaly:
            health_score *= 0.5  # Halve the score for anomalous nodes
        
        # Apply trend adjustment
        if trend == "degrading":
            health_score *= 0.9
        elif trend == "improving":
            health_score *= 1.05
        
        # Clamp to valid range
        health_score = max(0.01, min(1.0, health_score))
        
        return NodeHealth(
            node=node,
            health_score=health_score,
            current_latency=current_latency,
            predicted_latency=predicted_latency,
            error_rate=error_rate,
            is_anomaly=is_anomaly,
            anomaly_score=anomaly_score,
            trend=trend,
            confidence=prediction_confidence
        )
    
    def _latency_to_score(self, latency: float) -> float:
        """
        Convert latency to a score using exponential decay.
        
        e^(-k * latency)
        - k = 20: sensitivity factor
        - latency = 0.01s (10ms): score ≈ 0.82
        - latency = 0.05s (50ms): score ≈ 0.37
        - latency = 0.1s (100ms): score ≈ 0.14
        """
        if latency <= 0:
            return 1.0
        return math.exp(-self.sensitivity_factor * latency)
    
    def calculate_simple_score(self, latency: float) -> float:
        """
        Simple fallback scoring when Prophet is not available.
        """
        if latency <= 0:
            return 0.5  # Default for new nodes
        return self._latency_to_score(latency)
