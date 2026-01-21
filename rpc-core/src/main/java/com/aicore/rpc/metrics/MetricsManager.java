package com.aicore.rpc.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author YourName
 * @date 2023-10-28
 *
 * 指标管理器
 *
 * 职责:
 * 1. 提供一个全局唯一的 MeterRegistry 实例，用于整个RPC框架注册和记录指标。
 * 2. 默认使用 SimpleMeterRegistry，这是一个内存级的实现，用于测试或在没有配置
 *    具体监控系统时的兜底。后续可以替换为 PrometheusMeterRegistry 等具体实现。
 *
 * 设计说明:
 * 采用单例模式，确保所有组件都向同一个注册表报告指标，从而保证数据的统一性。
 */
public class MetricsManager {

    // 使用 volatile 和双重检查锁定来确保线程安全的单例
    private static volatile MeterRegistry meterRegistry;

    private MetricsManager() {
    }

    /**
     * 获取全局的 MeterRegistry 实例。
     * 如果还没有配置具体的 Registry，则默认创建一个 SimpleMeterRegistry。
     * @return MeterRegistry 实例
     */
    public static MeterRegistry getMeterRegistry() {
        if (meterRegistry == null) {
            synchronized (MetricsManager.class) {
                if (meterRegistry == null) {
                    // 默认实现，后续可以在应用启动时替换它
                    meterRegistry = new SimpleMeterRegistry();
                }
            }
        }
        return meterRegistry;
    }

    /**
     * 允许外部（如应用程序启动时）设置一个具体的 MeterRegistry 实现。
     * @param registry 具体的 MeterRegistry 实例，例如 PrometheusMeterRegistry。
     */
    public static void setMeterRegistry(MeterRegistry registry) {
        synchronized (MetricsManager.class) {
            meterRegistry = registry;
        }
    }
}