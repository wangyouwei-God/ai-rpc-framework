"""
Prometheus Metrics Collector
Fetches latency, error rate, and other metrics from Prometheus.
"""
import requests
import urllib.parse
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
import pandas as pd

from config import PROMETHEUS_URL


class PrometheusCollector:
    """Collects and processes metrics from Prometheus."""
    
    def __init__(self, prometheus_url: str = PROMETHEUS_URL):
        self.prometheus_url = prometheus_url
        self.session = requests.Session()
        self.session.headers.update({"Accept": "application/json"})
    
    def query(self, promql: str) -> Optional[List[dict]]:
        """Execute an instant query against Prometheus."""
        encoded_query = urllib.parse.quote(promql)
        url = f"{self.prometheus_url}/api/v1/query?query={encoded_query}"
        
        try:
            response = self.session.get(url, timeout=5)
            response.raise_for_status()
            result = response.json()
            
            if result.get("status") == "success" and result.get("data", {}).get("result"):
                return result["data"]["result"]
            return None
        except requests.exceptions.RequestException as e:
            print(f"Prometheus query error: {e}")
            return None
    
    def query_range(self, promql: str, start: datetime, end: datetime, 
                    step: str = "1m") -> Optional[pd.DataFrame]:
        """Execute a range query and return as DataFrame."""
        params = {
            "query": promql,
            "start": start.isoformat() + "Z",
            "end": end.isoformat() + "Z",
            "step": step
        }
        url = f"{self.prometheus_url}/api/v1/query_range"
        
        try:
            response = self.session.get(url, params=params, timeout=30)
            response.raise_for_status()
            result = response.json()
            
            if result.get("status") != "success":
                return None
            
            data = result.get("data", {}).get("result", [])
            if not data:
                return None
            
            # Convert to DataFrame suitable for Prophet
            records = []
            for series in data:
                instance = series.get("metric", {}).get("instance", "unknown")
                for timestamp, value in series.get("values", []):
                    records.append({
                        "ds": datetime.fromtimestamp(float(timestamp)),
                        "y": float(value),
                        "instance": instance
                    })
            
            return pd.DataFrame(records)
        except Exception as e:
            print(f"Prometheus range query error: {e}")
            return None
    
    def get_current_latency(self, nodes: List[str]) -> Dict[str, float]:
        """Get current average latency for each node."""
        query = (
            'sum(rate(rpc_server_processing_seconds_sum{job="rpc_provider"}[1m])) by (instance) '
            '/ '
            'sum(rate(rpc_server_processing_seconds_count{job="rpc_provider"}[1m])) by (instance)'
        )
        
        results = self.query(query)
        if not results:
            return {}
        
        latency_map = {}
        for result in results:
            instance = result.get("metric", {}).get("instance", "")
            instance = instance.replace("host.docker.internal", "127.0.0.1")
            value = float(result.get("value", [0, 0])[1])
            latency_map[instance] = value
        
        return {node: latency_map.get(node, 0) for node in nodes}
    
    def get_error_rate(self, nodes: List[str]) -> Dict[str, float]:
        """Get error rate for each node."""
        # Total calls
        total_query = 'sum(rate(rpc_server_requests_total{job="rpc_provider"}[1m])) by (instance)'
        # Error calls
        error_query = 'sum(rate(rpc_server_requests_total{job="rpc_provider",status="error"}[1m])) by (instance)'
        
        total_results = self.query(total_query)
        error_results = self.query(error_query)
        
        total_map = self._results_to_map(total_results)
        error_map = self._results_to_map(error_results)
        
        error_rates = {}
        for node in nodes:
            total = total_map.get(node, 0)
            errors = error_map.get(node, 0)
            error_rates[node] = errors / total if total > 0 else 0
        
        return error_rates
    
    def get_historical_latency(self, node: str, hours: int = 24) -> Optional[pd.DataFrame]:
        """Get historical latency data for Prophet training."""
        end = datetime.utcnow()
        start = end - timedelta(hours=hours)
        
        query = f'''
            sum(rate(rpc_server_processing_seconds_sum{{job="rpc_provider",instance=~".*{node}.*"}}[1m]))
            /
            sum(rate(rpc_server_processing_seconds_count{{job="rpc_provider",instance=~".*{node}.*"}}[1m]))
        '''
        
        df = self.query_range(query, start, end, step="1m")
        if df is not None and not df.empty:
            # Prophet requires 'ds' and 'y' columns
            return df[["ds", "y"]].dropna()
        return None
    
    def _results_to_map(self, results: Optional[List[dict]]) -> Dict[str, float]:
        """Convert Prometheus results to a dict."""
        if not results:
            return {}
        
        result_map = {}
        for result in results:
            instance = result.get("metric", {}).get("instance", "")
            instance = instance.replace("host.docker.internal", "127.0.0.1")
            value = float(result.get("value", [0, 0])[1])
            result_map[instance] = value
        
        return result_map
    
    def is_available(self) -> bool:
        """Check if Prometheus is reachable."""
        try:
            response = self.session.get(f"{self.prometheus_url}/-/healthy", timeout=2)
            return response.status_code == 200
        except:
            return False
