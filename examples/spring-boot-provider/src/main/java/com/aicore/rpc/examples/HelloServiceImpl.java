package com.aicore.rpc.examples;

import com.aicore.rpc.api.HelloService;
import com.aicore.rpc.spring.annotation.AiRpcService;

@AiRpcService
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " (from Spring Boot)";
    }
}
