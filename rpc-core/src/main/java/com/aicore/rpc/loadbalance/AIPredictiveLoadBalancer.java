package com.aicore.rpc.loadbalance;

import com.aicore.rpc.config.ConfigManager;
import com.aicore.rpc.loadbalance.ClientMetricsCollector.EndpointMetrics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 *
 * 基于AI预测的负载均衡策略
 *
 * 工作流程:
 * 1. 启动一个后台守护线程，每隔10秒定期调用AI预测服务，获取所有已知节点的健康分数（权重）。
 * 2. 将获取到的权重原子性地更新到本地缓存中。
 * 3. 首次调用或节点列表发生变化时，会记录最新的节点列表，供后台线程使用。
 * 4. select方法使用“加权随机”算法根据缓存的权重来选择一个节点。
 * 5. 如果AI服务请求失败，则自动降级为简单的随机负载均衡，保证高可用性。
 */
public class AIPredictiveLoadBalancer implements LoadBalancer {

    private static final Logger logger = LoggerFactory.getLogger(AIPredictiveLoadBalancer.class);

    private final String aiServiceUrl;
    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final Random random = new Random();

    // 使用 volatile 确保多线程环境下的可见性
    // 缓存从AI服务获取的权重
    private volatile Map<InetSocketAddress, Double> weightCache = new ConcurrentHashMap<>();
    // 缓存当前已知的服务地址列表
    private volatile List<InetSocketAddress> knownAddresses = new ArrayList<>();

    public AIPredictiveLoadBalancer() {
        // 从配置文件读取AI服务地址
        this.aiServiceUrl = ConfigManager.getString("rpc.loadbalancer.ai.service.url", "http://localhost:8000/predict");

        // 创建一个单线程的、可调度的执行器
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-loadbalancer-updater");
            // 设置为守护线程，这样当JVM退出时，不会因为这个线程还在运行而无法退出
            thread.setDaemon(true);
            return thread;
        });

        // 安排一个周期性任务：启动后延迟5秒开始第一次更新，之后每10秒执行一次
        scheduler.scheduleAtFixedRate(this::updateWeights, 5, 10, TimeUnit.SECONDS);
    }

    /**
     * 由后台线程定期调用的权重更新方法。
     */
    private void updateWeights() {
        // 必须检查 knownAddresses 是否为空，避免在服务还未被发现时就发起无效请求
        List<InetSocketAddress> currentAddresses = this.knownAddresses;
        if (currentAddresses.isEmpty()) {
            return; // 如果还没有已知的服务地址，则跳过本次更新
        }
        logger.debug("Starting scheduled weight update for {} nodes", currentAddresses.size());
        Map<InetSocketAddress, Double> newWeights = fetchWeightsFromAIService(currentAddresses);
        // 原子性地替换整个缓存，确保select方法总能读到完整、一致的权重集
        this.weightCache = newWeights;
    }

    @Override
    public InetSocketAddress select(List<InetSocketAddress> serviceAddresses) {
        if (serviceAddresses == null || serviceAddresses.isEmpty()) {
            return null;
        }
        if (serviceAddresses.size() == 1) {
            return serviceAddresses.get(0);
        }

        this.knownAddresses = serviceAddresses;
        Map<InetSocketAddress, Double> currentWeights = this.weightCache;

        if (currentWeights.isEmpty()) {
            logger.debug("Cache is empty on first call. Fetching weights immediately...");
            currentWeights = fetchWeightsFromAIService(serviceAddresses);
            this.weightCache = currentWeights;
        }

        // 1. Collect client-side metrics for local weight adjustment
        ClientMetricsCollector collector = ClientMetricsCollector.getInstance();
        Map<InetSocketAddress, EndpointMetrics> clientMetrics = collector.collectMetrics("", serviceAddresses);

        // 2. Calculate final weights by combining AI weights with local metrics
        Map<InetSocketAddress, Double> finalWeights = new ConcurrentHashMap<>();
        for (InetSocketAddress address : serviceAddresses) {
            double aiWeight = currentWeights.getOrDefault(address, 1.0);

            // Apply local weight adjustment based on circuit breaker and latency
            EndpointMetrics metrics = clientMetrics.get(address);
            double localMultiplier = 1.0;
            if (metrics != null) {
                localMultiplier = collector.calculateLocalWeight(metrics);
            }

            // Final weight = AI weight * local multiplier
            double finalWeight = aiWeight * localMultiplier;
            finalWeights.put(address, finalWeight);
        }

        // 3. Calculate total weight
        double totalWeight = finalWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight <= 0) {
            // All nodes are unhealthy, fallback to random
            return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
        }

        // 4. Weighted random selection
        double randomPoint = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;
        for (Map.Entry<InetSocketAddress, Double> entry : finalWeights.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomPoint < cumulativeWeight) {
                logger.debug("Selected: {} (AI Weight: {}, Final: {})",
                        entry.getKey(), currentWeights.getOrDefault(entry.getKey(), 1.0), entry.getValue());
                return entry.getKey();
            }
        }
        // Fallback
        return serviceAddresses.get(serviceAddresses.size() - 1);
    }

    /**
     * 调用Python AI服务，获取节点权重。
     * 
     * @param addresses 当前可用的服务地址列表
     * @return 一个从地址到权重的映射
     */
    private Map<InetSocketAddress, Double> fetchWeightsFromAIService(List<InetSocketAddress> addresses) {
        List<String> nodeList = addresses.stream()
                .map(address -> address.getHostString() + ":" + address.getPort())
                .collect(Collectors.toList());

        String jsonBody = gson.toJson(nodeList);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(this.aiServiceUrl).post(body).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected HTTP code " + response);
            }
            String responseBody = response.body().string();
            Type type = new TypeToken<Map<String, Double>>() {
            }.getType();
            Map<String, Double> stringWeights = gson.fromJson(responseBody, type);

            Map<InetSocketAddress, Double> result = new ConcurrentHashMap<>();
            for (InetSocketAddress address : addresses) {
                String key = address.getHostString() + ":" + address.getPort();
                // 使用AI服务返回的权重，如果没有则默认1.0
                result.put(address, stringWeights.getOrDefault(key, 1.0));
            }
            logger.debug("Fetched new weights: {}", result);
            return result;
        } catch (IOException e) {
            logger.warn("Failed to fetch weights from AI service: {}. Falling back to equal weights.", e.getMessage());
            // 降级策略：如果请求AI服务失败，则认为所有节点权重相等（退化为随机负载均衡）
            return addresses.stream()
                    .collect(Collectors.toMap(addr -> addr, addr -> 1.0, (o1, o2) -> o1, ConcurrentHashMap::new));
        }
    }
}