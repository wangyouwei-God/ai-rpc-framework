package com.aicore.rpc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L; // 确保序列化兼容性
    private String requestId;      // 请求ID
    private String className;      // 目标类名
    private String methodName;     // 目标方法名
    private Class<?>[] paramTypes; // 参数类型
    private Object[] params;       // 参数值
    private boolean heartbeat = false; // 新增心跳标识
}
