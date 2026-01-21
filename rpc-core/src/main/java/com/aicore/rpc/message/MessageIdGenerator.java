package com.aicore.rpc.message;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程安全的消息ID生成器。
 *
 * 设计目的:
 * 为每一个RPC请求生成一个全局唯一的ID。此ID用于异步通信中请求与响应的匹配。
 * 客户端在发送请求时携带一个ID，服务端在处理完后将此ID原样返回，
 * 客户端根据ID即可唤醒等待相应请求结果的线程。
 *
 * 实现方式:
 * 使用 `java.util.concurrent.atomic.AtomicInteger` 保证在高并发场景下ID生成的原子性和线程安全性，
 * 避免了使用 `synchronized` 关键字带来的性能开销。
 */
public class MessageIdGenerator {

    // 使用AtomicInteger作为底层计数器，初始值为0。
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * 获取下一个唯一的消息ID。
     * 这是一个静态方法，方便全局调用。
     *
     * @return 返回一个原子性递增后的整数，作为唯一的消息ID。
     */
    public static int nextId() {
        // `incrementAndGet()` 方法会原子性地将当前值加1，并返回更新后的值。
        return idGenerator.incrementAndGet();
    }
}