from fastapi import FastAPI
import requests
import time
import urllib.parse
import math # 引入 math 库

# --- 配置 ---
PROMETHEUS_URL = "http://localhost:9090"

app = FastAPI(
    title="AI Forecasting Service for RPC Load Balancing",
    description="Predicts future node health based on real-time metrics from Prometheus.",
    version="2.1.0", # 版本升级
)

def query_prometheus(query: str):
    """向 Prometheus 发送查询请求"""
    encoded_query = urllib.parse.quote(query)
    url = f"{PROMETHEUS_URL}/api/v1/query?query={encoded_query}"
    print(f"Querying Prometheus: {url}")
    try:
        response = requests.get(url, timeout=5)
        response.raise_for_status()
        result = response.json()
        if result.get("status") == "success" and result.get("data", {}).get("result"):
            return result["data"]["result"]
        print(f"Prometheus returned success status but no result data. Response: {result}")
        return None
    except requests.exceptions.RequestException as e:
        print(f"Error querying Prometheus: {e}")
        return None

@app.post("/predict")
async def predict_health_score(nodes: list[str]):
    """
    接收一个节点列表，为每个节点预测一个健康分数。
    """
    print(f"Received prediction request for nodes: {nodes}")
    
    # 预测模型：使用过去1分钟的平均延迟作为预测值
    query = (
        'sum(rate(rpc_server_processing_seconds_sum{job="rpc_provider"}[1m])) by (instance) '
        '/ '
        'sum(rate(rpc_server_processing_seconds_count{job="rpc_provider"}[1m])) by (instance)'
    )

    results = query_prometheus(query)
    
    if not results:
        print("Failed to get metrics from Prometheus. Returning default scores.")
        return {node: 0.5 for node in nodes}

    # 将查询结果处理成一个字典: { "ip:port": latency }
    latency_map = {
        result["metric"]["instance"].replace("host.docker.internal", "127.0.0.1"): float(result["value"][1])
        for result in results
    }
    print(f"Current latency map from Prometheus: {latency_map}")
    
    predictions = {}
    for node in nodes:
        latency = latency_map.get(node)
        
        if latency is None or latency <= 0:
            # 对于没有延迟数据的新节点，给一个最高的权重1.0，鼓励系统使用它
            health_score = 1.0
        else:
            # 关键修改：使用指数衰减函数，这是一个对延迟变化更敏感的权重计算公式
            # e^(-k*x)，其中x是延迟，k是敏感度因子
            sensitivity_factor = 20
            health_score = math.exp(-sensitivity_factor * latency)
        
        predictions[node] = round(health_score, 4)

    print(f"Predicted health scores: {predictions}")
    return predictions

@app.get("/health")
async def health_check():
    """提供一个 /health 接口，用于检查服务是否存活"""
    return {"status": "ok"}
