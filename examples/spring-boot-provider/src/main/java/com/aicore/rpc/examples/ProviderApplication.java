package com.aicore.rpc.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.aicore.rpc.spring.annotation.EnableAiRpc;

@SpringBootApplication
@EnableAiRpc
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
