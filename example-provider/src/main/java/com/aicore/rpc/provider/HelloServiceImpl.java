package com.aicore.rpc.provider;

import com.aicore.rpc.api.HelloService;

public class HelloServiceImpl implements HelloService {

    // 1. 新增一个成员变量，用于保存当前实例的端口号
    private final int port;

    /**
     * 2. 新增的构造函数。
     *    当创建 HelloServiceImpl 实例时，必须传入一个端口号。
     * @param port 当前服务实例监听的 RPC 端口
     */
    public HelloServiceImpl(int port) {
        this.port = port;
    }

    /**
     * 实现 HelloService 接口的方法。
     * @param name 客户端传来的参数
     * @return 包含端口信息的问候语
     */
    @Override
    public String sayHello(String name) {
        // 打印日志，方便从控制台确认哪个实例收到了请求
        System.out.println("Instance on port " + this.port + " received call: sayHello(" + name + ")");

        // ====================== 实验代码：注入延迟 ======================
        // 如果当前实例是监听 8888 端口的那个 Provider
        if (this.port == 8888) {
            try {
                // 让当前线程睡眠 200 毫秒。
                // 这会直接增加本次RPC调用的服务端处理耗时。
                System.out.println("    -> Injecting 200ms delay on port 8888...");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // 在多线程环境中，捕获 InterruptedException 并恢复中断状态
                Thread.currentThread().interrupt();
            }
        }
        // ================================================================

        return "Hello, " + name + " -> response from port:" + this.port;
    }
}