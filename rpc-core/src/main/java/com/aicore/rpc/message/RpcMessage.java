package com.aicore.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcMessage {
    private byte messageType;
    private byte serializerType;
    private int messageId;
    private Object data; // RpcRequest or RpcResponse

    // 分布式追踪：用于上下文传播的头部
    @Builder.Default // Lombok Builder注解，如果创建时不指定，则默认为new HashMap<>()
    private Map<String, String> attachments = new HashMap<>();
}