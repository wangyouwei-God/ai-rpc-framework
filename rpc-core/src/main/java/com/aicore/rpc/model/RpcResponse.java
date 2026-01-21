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
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private String requestId;  // 对应的请求ID
    private Object result;     // 调用结果
    private Throwable error;   // 如果发生错误
}
