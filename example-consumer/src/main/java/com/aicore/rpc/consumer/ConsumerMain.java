package com.aicore.rpc.consumer;

import com.aicore.rpc.api.HelloService;
import com.aicore.rpc.loadbalance.AIPredictiveLoadBalancer;
import com.aicore.rpc.loadbalance.LoadBalancer;
import com.aicore.rpc.proxy.RpcProxy;
import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.registry.nacos.NacosRegistry;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

public class ConsumerMain {
    private static final String NACOS_ADDRESS = "127.0.0.1:8848";

    public static void main(String[] args) {
        System.setProperty("nacos.remote.client.grpc.tls.enable", "false");
        try {
            PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            prometheusRegistry.config().meterFilter(
                    new MeterFilter() {
                        @Override
                        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                            if (id.getName().startsWith("rpc.")) {
                                return DistributionStatisticConfig.builder()
                                        .percentilesHistogram(true) // 启用直方图
                                        .sla(Duration.ofMillis(5).toNanos(),   // 设置 SLA 桶，用于更精确的统计
                                                Duration.ofMillis(10).toNanos(),
                                                Duration.ofMillis(25).toNanos(),
                                                Duration.ofMillis(50).toNanos(),
                                                Duration.ofMillis(100).toNanos())
                                        .build()
                                        .merge(config);
                            }
                            return config;
                        }
                    });

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8082), 0);
            server.createContext("/metrics", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            new Thread(server::start).start();
            System.out.println("Metrics server started on http://localhost:8082/metrics");

            Registry registry = new NacosRegistry(NACOS_ADDRESS);
            // LoadBalancer loadBalancer = new RandomLoadBalancer();
            LoadBalancer loadBalancer = new AIPredictiveLoadBalancer();

            // 将配置好的 prometheusRegistry 实例注入
            HelloService helloService = RpcProxy.create(HelloService.class, registry, loadBalancer, prometheusRegistry);

            int count = 0;
            while (true) {
                try {
                    String result = helloService.sayHello("AI RPC " + (count++));
                    System.out.println("RPC Call | Result: " + result);
                } catch (Exception e) {
                    System.err.println("RPC Call | Error: " + e.getMessage());
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}