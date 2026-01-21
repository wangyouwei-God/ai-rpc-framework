"""
Prophet-based Time Series Forecasting Model
Provides latency prediction with progressive learning.
"""
from datetime import datetime, timedelta
from typing import Dict, Optional, Tuple
import pandas as pd
import numpy as np
from threading import Lock
import logging

try:
    from prophet import Prophet
    PROPHET_AVAILABLE = True
except ImportError:
    PROPHET_AVAILABLE = False
    print("Warning: Prophet not installed. Using fallback mode.")

from config import PROPHET_CONFIG, MIN_DATA_POINTS_FOR_PROPHET, PREDICTION_HORIZON_MINUTES

logger = logging.getLogger(__name__)


class ProphetModel:
    """
    Prophet-based forecasting with progressive learning.
    Adapts confidence based on data availability.
    """
    
    def __init__(self, node: str):
        self.node = node
        self.model: Optional[Prophet] = None
        self.last_trained: Optional[datetime] = None
        self.training_data: Optional[pd.DataFrame] = None
        self.data_points_count = 0
        self.lock = Lock()
        self.last_mape: float = 1.0  # Mean Absolute Percentage Error
    
    def train(self, df: pd.DataFrame) -> bool:
        """
        Train Prophet model on historical data.
        
        Args:
            df: DataFrame with 'ds' (datetime) and 'y' (latency) columns
        
        Returns:
            True if training succeeded
        """
        if not PROPHET_AVAILABLE:
            logger.warning("Prophet not available, skipping training")
            return False
        
        if df is None or len(df) < MIN_DATA_POINTS_FOR_PROPHET:
            logger.info(f"Insufficient data for training: {len(df) if df is not None else 0} points")
            return False
        
        with self.lock:
            try:
                # Suppress Prophet logging
                import logging
                logging.getLogger('prophet').setLevel(logging.WARNING)
                logging.getLogger('cmdstanpy').setLevel(logging.WARNING)
                
                # Create and configure Prophet model
                self.model = Prophet(
                    changepoint_prior_scale=PROPHET_CONFIG["changepoint_prior_scale"],
                    seasonality_mode=PROPHET_CONFIG["seasonality_mode"],
                    daily_seasonality=PROPHET_CONFIG["daily_seasonality"],
                    weekly_seasonality=PROPHET_CONFIG["weekly_seasonality"],
                    uncertainty_samples=PROPHET_CONFIG["uncertainty_samples"]
                )
                
                # Train
                self.model.fit(df)
                self.training_data = df
                self.data_points_count = len(df)
                self.last_trained = datetime.utcnow()
                
                # Calculate in-sample MAPE for confidence estimation
                self._calculate_mape(df)
                
                logger.info(f"Model trained for {self.node} with {len(df)} points, MAPE={self.last_mape:.4f}")
                return True
                
            except Exception as e:
                logger.error(f"Training failed for {self.node}: {e}")
                return False
    
    def predict(self, horizon_minutes: int = PREDICTION_HORIZON_MINUTES) -> Tuple[float, float]:
        """
        Predict future latency.
        
        Returns:
            Tuple of (predicted_latency, confidence)
        """
        if not PROPHET_AVAILABLE or self.model is None:
            return 0.0, 0.0
        
        with self.lock:
            try:
                # Create future DataFrame
                future = self.model.make_future_dataframe(
                    periods=horizon_minutes, 
                    freq='min'
                )
                
                # Predict
                forecast = self.model.predict(future)
                
                # Get the prediction for the target horizon
                prediction = forecast.iloc[-1]
                predicted_latency = max(0, prediction['yhat'])
                
                # Calculate confidence based on uncertainty interval and MAPE
                if 'yhat_upper' in prediction and 'yhat_lower' in prediction:
                    uncertainty_range = prediction['yhat_upper'] - prediction['yhat_lower']
                    base_confidence = max(0, 1 - (uncertainty_range / (predicted_latency + 0.001)))
                else:
                    base_confidence = 0.5
                
                # Adjust confidence by MAPE and data availability
                data_confidence = self._get_data_confidence()
                mape_confidence = max(0, 1 - self.last_mape)
                
                confidence = base_confidence * data_confidence * mape_confidence
                confidence = max(0.1, min(0.99, confidence))
                
                return predicted_latency, confidence
                
            except Exception as e:
                logger.error(f"Prediction failed for {self.node}: {e}")
                return 0.0, 0.0
    
    def get_trend(self) -> str:
        """
        Get the trend direction: 'improving', 'stable', 'degrading'.
        """
        if self.model is None or self.training_data is None:
            return "unknown"
        
        try:
            # Compare recent predictions with historical average
            recent = self.training_data.tail(10)['y'].mean()
            overall = self.training_data['y'].mean()
            
            ratio = recent / overall if overall > 0 else 1
            
            if ratio < 0.9:
                return "improving"
            elif ratio > 1.1:
                return "degrading"
            else:
                return "stable"
        except:
            return "unknown"
    
    def _calculate_mape(self, df: pd.DataFrame):
        """Calculate Mean Absolute Percentage Error on training data."""
        try:
            forecast = self.model.predict(df)
            actual = df['y'].values
            predicted = forecast['yhat'].values
            
            # Avoid division by zero
            mask = actual > 0.001
            if mask.sum() > 0:
                mape = np.mean(np.abs((actual[mask] - predicted[mask]) / actual[mask]))
                self.last_mape = min(1.0, mape)
            else:
                self.last_mape = 0.5
        except:
            self.last_mape = 0.5
    
    def _get_data_confidence(self) -> float:
        """
        Calculate confidence based on data availability.
        More data = higher confidence.
        """
        if self.data_points_count == 0:
            return 0.0
        
        # 1 hour (60 points) = 50% confidence
        # 24 hours (1440 points) = 90% confidence
        # 1 week (10080 points) = 99% confidence
        
        if self.data_points_count < 60:
            return 0.1 + (self.data_points_count / 60) * 0.4
        elif self.data_points_count < 1440:
            return 0.5 + ((self.data_points_count - 60) / 1380) * 0.4
        else:
            return min(0.99, 0.9 + (self.data_points_count / 100000))
    
    def is_trained(self) -> bool:
        """Check if model is trained and ready."""
        return self.model is not None and self.last_trained is not None
    
    def needs_retrain(self, hours: int = 6) -> bool:
        """Check if model needs retraining."""
        if self.last_trained is None:
            return True
        return (datetime.utcnow() - self.last_trained) > timedelta(hours=hours)


class ProphetModelRegistry:
    """
    Registry for managing Prophet models per node.
    """
    
    def __init__(self):
        self.models: Dict[str, ProphetModel] = {}
        self.lock = Lock()
    
    def get_or_create(self, node: str) -> ProphetModel:
        """Get existing model or create new one for a node."""
        with self.lock:
            if node not in self.models:
                self.models[node] = ProphetModel(node)
            return self.models[node]
    
    def get(self, node: str) -> Optional[ProphetModel]:
        """Get model for a node if exists."""
        return self.models.get(node)
    
    def list_nodes(self) -> list:
        """List all nodes with models."""
        return list(self.models.keys())
