package com.aicore.rpc.provider;

import com.aicore.rpc.config.ConfigManager;
import com.aicore.rpc.metrics.MetricsManager;
import com.aicore.rpc.registry.Registry;
import com.aicore.rpc.registry.nacos.NacosRegistry;
import com.aicore.rpc.server.RpcServer;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

public class ProviderMain {
    private static final String NACOS_ADDRESS = "127.0.0.1:8848";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ProviderMain <rpcPort> <metricsPort>");
            System.exit(1);
        }
        int rpcPort = Integer.parseInt(args[0]);
        int metricsPort = Integer.parseInt(args[1]);

        try {
            // 1. 初始化 Nacos 连接
            // Registry registry = new NacosRegistry(NACOS_ADDRESS);
             String nacosAddress = ConfigManager.getString("rpc.registry.address", "127.0.0.1:8848");
             Registry registry = new NacosRegistry(nacosAddress);
            // 2. 初始化 Metrics 系统
            PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            MetricsManager.setMeterRegistry(prometheusRegistry);
            prometheusRegistry.config().meterFilter(
                    new MeterFilter() {
                        @Override
                        public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                            if (id.getName().startsWith("rpc.")) {
                                return DistributionStatisticConfig.builder()
                                        .percentilesHistogram(true)
                                        .sla(Duration.ofMillis(1).toNanos(), Duration.ofMillis(5).toNanos(),
                                                Duration.ofMillis(10).toNanos(), Duration.ofMillis(25).toNanos())
                                        .build().merge(config);
                            }
                            return config;
                        }
                    });

            HttpServer metricsServer = HttpServer.create(new InetSocketAddress("0.0.0.0", metricsPort), 0);
            metricsServer.createContext("/metrics", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            new Thread(metricsServer::start).start();
            System.out.println("Metrics server started on http://localhost:" + metricsPort + "/metrics");

            // 3. 初始化 RPC Server
            RpcServer server = new RpcServer(rpcPort, registry);

            // 4. 关键步骤：注册一个 JVM Shutdown Hook
            // 当 JVM 收到关闭信号时（如 Ctrl+C），这个钩子里的代码会被执行。
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered. Starting graceful shutdown...");
                server.shutdown();
            }));

            // 5. 注册服务并启动服务器
            server.registerService(new HelloServiceImpl(rpcPort));
            server.start();

            // 6. 阻塞主线程，直到服务器的 Channel 关闭
            // `server.start()` 是非阻塞的，需要这行代码来防止 main 方法立即退出。
            // 当 shutdown hook 被触发并关闭 channel 时，这里的 sync() 就会返回，主线程结束。
            server.getChannelFuture().channel().closeFuture().sync();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}