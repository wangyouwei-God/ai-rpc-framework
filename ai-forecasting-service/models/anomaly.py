"""
Anomaly Detection using Isolation Forest.
Detects unusual latency patterns that indicate potential issues.
"""
from typing import Dict, List, Optional
import numpy as np
from collections import deque
from threading import Lock
import logging

try:
    from sklearn.ensemble import IsolationForest
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False
    print("Warning: scikit-learn not installed. Anomaly detection disabled.")

from config import ANOMALY_CONTAMINATION

logger = logging.getLogger(__name__)


class AnomalyDetector:
    """
    Isolation Forest based anomaly detector for latency data.
    Maintains a sliding window of recent observations.
    """
    
    def __init__(self, node: str, window_size: int = 100):
        self.node = node
        self.window_size = window_size
        self.observations: deque = deque(maxlen=window_size)
        self.model: Optional[IsolationForest] = None
        self.lock = Lock()
        self.is_trained = False
        self.min_samples = 20  # Minimum samples before detection
    
    def add_observation(self, latency: float, error_rate: float = 0.0):
        """Add a new observation to the sliding window."""
        with self.lock:
            self.observations.append({
                "latency": latency,
                "error_rate": error_rate
            })
            
            # Retrain if we have enough new data
            if len(self.observations) >= self.min_samples:
                self._train()
    
    def is_anomaly(self, latency: float, error_rate: float = 0.0) -> tuple:
        """
        Check if the current observation is anomalous.
        
        Returns:
            Tuple of (is_anomaly: bool, anomaly_score: float)
            anomaly_score ranges from 0 (normal) to 1 (highly anomalous)
        """
        if not SKLEARN_AVAILABLE or not self.is_trained:
            return False, 0.0
        
        with self.lock:
            try:
                X = np.array([[latency, error_rate]])
                
                # Get prediction (-1 = anomaly, 1 = normal)
                prediction = self.model.predict(X)[0]
                
                # Get anomaly score (more negative = more anomalous)
                raw_score = self.model.score_samples(X)[0]
                
                # Convert to 0-1 range (higher = more anomalous)
                # Typical scores range from -0.5 (normal) to -1.0 (anomaly)
                anomaly_score = max(0, min(1, -raw_score - 0.3))
                
                is_anomaly = prediction == -1
                
                return is_anomaly, anomaly_score
                
            except Exception as e:
                logger.error(f"Anomaly detection failed for {self.node}: {e}")
                return False, 0.0
    
    def _train(self):
        """Train the Isolation Forest model on recent observations."""
        if not SKLEARN_AVAILABLE:
            return
        
        try:
            # Prepare training data
            X = np.array([
                [obs["latency"], obs["error_rate"]] 
                for obs in self.observations
            ])
            
            # Train Isolation Forest
            self.model = IsolationForest(
                contamination=ANOMALY_CONTAMINATION,
                random_state=42,
                n_estimators=100
            )
            self.model.fit(X)
            self.is_trained = True
            
        except Exception as e:
            logger.error(f"Anomaly detector training failed for {self.node}: {e}")
            self.is_trained = False


class AnomalyDetectorRegistry:
    """Registry for managing anomaly detectors per node."""
    
    def __init__(self):
        self.detectors: Dict[str, AnomalyDetector] = {}
        self.lock = Lock()
    
    def get_or_create(self, node: str) -> AnomalyDetector:
        """Get existing detector or create new one for a node."""
        with self.lock:
            if node not in self.detectors:
                self.detectors[node] = AnomalyDetector(node)
            return self.detectors[node]
    
    def get(self, node: str) -> Optional[AnomalyDetector]:
        """Get detector for a node if exists."""
        return self.detectors.get(node)
