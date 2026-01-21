# AI-RPC Framework 架构设计文档

## Architecture Design Document

---

## 1. 系统整体架构图

```mermaid
graph TB
    subgraph "Client Application"
        CA[Consumer App]
        ANN["@AiRpcReference"]
    end

    subgraph "AI-RPC Consumer"
        RP[RpcProxy]
        LB[AIPredictiveLoadBalancer]
        CB[CircuitBreaker]
        AT[AdaptiveTimeout]
        SR[SmartRetry]
        CP[ChannelPoolManager]
    end

    subgraph "Infrastructure"
        NC[Nacos Registry]
        PM[Prometheus]
        AI[AI Forecasting Service]
        GF[Grafana]
    end

    subgraph "AI-RPC Provider"
        RS[RpcServer]
        SH[RpcServerHandler]
        SR2[ServiceRegistry]
        MM[MetricsManager]
    end

    subgraph "Provider Application"
        PA[Provider App]
        ANN2["@AiRpcService"]
    end

    CA --> ANN
    ANN --> RP
    RP --> CB
    CB --> AT
    AT --> SR
    SR --> LB
    LB --> CP
    CP -->|"Netty TCP"| RS
    
    LB <-->|"HTTP/JSON"| AI
    AI <-->|"PromQL"| PM
    
    RS --> SH
    SH --> SR2
    SR2 --> PA
    PA --> ANN2
    
    SR2 -->|"Register"| NC
    LB -->|"Discover"| NC
    
    MM --> PM
    PM --> GF

    style AI fill:#f9f,stroke:#333,stroke-width:2px
    style LB fill:#bbf,stroke:#333,stroke-width:2px
    style CB fill:#fbb,stroke:#333,stroke-width:2px
```

---

## 2. RPC调用完整流程图

```mermaid
sequenceDiagram
    autonumber
    participant C as Consumer
    participant P as RpcProxy
    participant CB as CircuitBreaker
    participant AT as AdaptiveTimeout
    participant SR as SmartRetry
    participant LB as LoadBalancer
    participant AI as AI Service
    participant CP as ChannelPool
    participant S as Provider

    C->>P: 调用业务方法
    P->>CB: 检查熔断状态
    
    alt 熔断器OPEN
        CB-->>P: 快速失败
        P-->>C: CircuitBreakerOpenException
    else 熔断器CLOSED/HALF_OPEN
        CB->>AT: 获取自适应超时
        AT->>SR: 包装重试逻辑
        
        loop 最多3次重试
            SR->>LB: 选择服务节点
            LB->>AI: 获取健康权重(异步)
            AI-->>LB: {node: score}
            LB-->>SR: 选中节点
            
            SR->>CP: 获取连接
            CP->>S: 发送RPC请求
            
            alt 成功
                S-->>CP: 响应结果
                CP-->>SR: 返回结果
                SR->>AT: 记录延迟
                SR->>CB: 记录成功
                SR-->>P: 返回结果
                P-->>C: 业务结果
            else 失败
                S-->>CP: 异常/超时
                CP-->>SR: 失败
                SR->>CB: 记录失败
                SR->>SR: 指数退避等待
            end
        end
    end
```

---

## 3. 模块架构详解

### 3.1 AI智能负载均衡模块

```mermaid
graph LR
    subgraph "AIPredictiveLoadBalancer"
        SEL[select方法]
        WC[WeightCache]
        BG[后台更新线程]
    end

    subgraph "AI Forecasting Service"
        PE["/predict 端点"]
        PM[ProphetModel]
        AD[AnomalyDetector]
        HS[HealthScorer]
        PC[PrometheusCollector]
    end

    subgraph "权重计算"
        L[Latency Score]
        E[Error Rate]
        T[Trend Prediction]
        A[Anomaly Score]
    end

    SEL -->|读取| WC
    BG -->|每10秒| PE
    PE -->|更新| WC
    
    PC -->|采集指标| PE
    PE --> PM
    PE --> AD
    PM --> HS
    AD --> HS
    
    HS --> L
    HS --> E
    HS --> T
    HS --> A
    
    L --> |0.35权重| HS
    E --> |0.25权重| HS
    T --> |0.25权重| HS
    A --> |0.15权重| HS
```

#### AI负载均衡流程

```mermaid
flowchart TD
    A[收到节点列表] --> B{Weight缓存是否为空?}
    B -->|是| C[同步获取AI权重]
    B -->|否| D[使用缓存权重]
    C --> D
    
    D --> E[获取客户端本地指标]
    E --> F[计算最终权重]
    F --> G[final = AI权重 × 本地乘数]
    
    G --> H{总权重 > 0?}
    H -->|否| I[随机选择]
    H -->|是| J[加权随机选择]
    
    J --> K[返回选中节点]
    I --> K

    style C fill:#f9f,stroke:#333
    style G fill:#bbf,stroke:#333
```

---

### 3.2 熔断器模块

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    
    CLOSED --> OPEN: 失败率 > 阈值 (50%)
    OPEN --> HALF_OPEN: 等待时间结束 (10s)
    HALF_OPEN --> CLOSED: 探测成功
    HALF_OPEN --> OPEN: 探测失败
    
    note right of CLOSED: 正常状态\n记录成功/失败
    note right of OPEN: 熔断状态\n快速失败
    note right of HALF_OPEN: 半开状态\n放行1个探测请求
```

#### 熔断器组件结构

```mermaid
classDiagram
    class CircuitBreaker {
        -CircuitBreakerState state
        -SlidingWindowMetrics metrics
        -CircuitBreakerConfig config
        +execute(Callable) Object
        +recordSuccess()
        +recordFailure()
    }
    
    class SlidingWindowMetrics {
        -long[] timestamps
        -boolean[] results
        +recordResult(boolean)
        +getFailureRate() double
        +getCallCount() int
    }
    
    class CircuitBreakerConfig {
        +failureRateThreshold: double
        +slidingWindowSize: int
        +waitDurationInOpenState: Duration
        +minimumNumberOfCalls: int
    }
    
    class CircuitBreakerRegistry {
        -Map~String,CircuitBreaker~ registry
        +getOrCreate(String) CircuitBreaker
    }
    
    CircuitBreaker *-- SlidingWindowMetrics
    CircuitBreaker *-- CircuitBreakerConfig
    CircuitBreakerRegistry o-- CircuitBreaker
```

---

### 3.3 自适应超时模块

```mermaid
flowchart LR
    subgraph "延迟采集"
        R1[请求1: 15ms]
        R2[请求2: 20ms]
        R3[请求3: 18ms]
        RN[请求N: ...]
    end

    subgraph "统计计算"
        SW[滑动窗口\n1000个样本]
        P99[P99计算\n排序取99%位]
    end

    subgraph "超时决策"
        AT[自适应超时]
        MUL[乘数: 1.5x]
        MIN[最小值: 100ms]
        MAX[最大值: 30s]
    end

    R1 --> SW
    R2 --> SW
    R3 --> SW
    RN --> SW
    
    SW --> P99
    P99 --> AT
    AT --> MUL
    MUL --> |"timeout = P99 × 1.5"| MIN
    MIN --> MAX
    MAX --> |最终超时值| OUT[应用超时]
```

---

### 3.4 智能重试模块

```mermaid
flowchart TD
    A[执行操作] --> B{成功?}
    B -->|是| C[返回结果]
    B -->|否| D{可重试异常?}
    
    D -->|否| E[抛出异常]
    D -->|是| F{重试次数 < 3?}
    
    F -->|否| E
    F -->|是| G[计算退避时间]
    
    G --> H["delay = base × 2^attempt"]
    H --> I[添加随机抖动 ±25%]
    I --> J[等待delay毫秒]
    J --> A
    
    style G fill:#ffd,stroke:#333
    style I fill:#ffd,stroke:#333
```

---

### 3.5 RPC通信协议

```mermaid
packet-beta
    0-31: "Magic Number (4 bytes): 0xCAFEBABE"
    32-39: "Version (1 byte)"
    40-47: "Serializer (1 byte)"
    48-55: "Message Type (1 byte)"
    56-87: "Request ID (4 bytes)"
    88-119: "Body Length (4 bytes)"
    120-159: "Body Data (N bytes)..."
```

#### 消息类型

```mermaid
graph LR
    subgraph "Message Types"
        REQ[REQUEST: 1]
        RES[RESPONSE: 2]
        HB[HEARTBEAT: 3]
    end

    subgraph "Serializers"
        PS[Protostuff: 1]
        JDK[JDK: 2]
    end
```

---

## 4. 数据流架构图

```mermaid
flowchart TB
    subgraph "数据采集层"
        P1[Provider 1\n:8081/metrics]
        P2[Provider 2\n:8082/metrics]
        P3[Provider 3\n:8083/metrics]
    end

    subgraph "存储层"
        PROM[(Prometheus\nTSDB)]
    end

    subgraph "分析层"
        AI[AI Forecasting Service]
        PROP[Prophet Model]
        ISO[Isolation Forest]
    end

    subgraph "决策层"
        LB[Load Balancer]
        WC[Weight Cache]
    end

    subgraph "可视化层"
        GRAF[Grafana Dashboard]
    end

    P1 -->|scrape| PROM
    P2 -->|scrape| PROM
    P3 -->|scrape| PROM
    
    PROM -->|PromQL| AI
    AI --> PROP
    AI --> ISO
    PROP --> |预测| AI
    ISO --> |异常检测| AI
    
    AI -->|健康分数| WC
    WC --> LB
    
    PROM -->|可视化| GRAF
```

---

## 5. 部署架构图

```mermaid
graph TB
    subgraph "Docker Compose"
        subgraph "基础设施"
            NC[Nacos\n:8848]
            PROM[Prometheus\n:9090]
            GRAF[Grafana\n:3000]
        end

        subgraph "AI服务"
            AIS[AI Forecasting\n:8000]
        end
    end

    subgraph "应用服务"
        P1[Provider 1\n:9091]
        P2[Provider 2\n:9092]
        C1[Consumer\n:8080]
    end

    C1 -->|RPC| P1
    C1 -->|RPC| P2
    C1 -->|HTTP| AIS
    
    P1 -->|Register| NC
    P2 -->|Register| NC
    C1 -->|Discover| NC
    
    P1 -->|Metrics| PROM
    P2 -->|Metrics| PROM
    AIS -->|Query| PROM
    
    PROM --> GRAF
```

---

## 6. Spring Boot 集成架构

```mermaid
graph TB
    subgraph "Spring Context"
        AC[AiRpcAutoConfiguration]
        PP[AiRpcProperties]
        
        subgraph "Processors"
            SP[ServiceProcessor]
            RP[ReferenceProcessor]
        end
        
        subgraph "Core Beans"
            LB[LoadBalancer]
            REG[Registry]
            SRV[RpcServer]
        end
    end

    subgraph "User Code"
        SVC["@AiRpcService\nHelloServiceImpl"]
        REF["@AiRpcReference\nHelloService proxy"]
    end

    AC --> PP
    AC --> SP
    AC --> RP
    AC --> LB
    AC --> REG
    AC --> SRV
    
    SP -->|扫描注册| SVC
    RP -->|创建代理| REF
    
    SVC -->|注册到| REG
    REF -->|使用| LB
```

---

## 7. 故障处理流程

```mermaid
flowchart TD
    A[RPC请求] --> B{节点健康?}
    B -->|是| C[正常处理]
    B -->|否| D[AI降低权重]
    
    C --> E{响应成功?}
    E -->|是| F[返回结果]
    E -->|否| G{熔断器状态?}
    
    G -->|OPEN| H[快速失败]
    G -->|CLOSED| I{可重试?}
    
    I -->|是| J[智能重试]
    I -->|否| K[抛出异常]
    
    J --> L{重试成功?}
    L -->|是| F
    L -->|否| M{达到上限?}
    
    M -->|是| K
    M -->|否| J
    
    D --> N[流量转移到健康节点]
    N --> C

    style D fill:#f9f,stroke:#333
    style J fill:#ffd,stroke:#333
    style H fill:#fbb,stroke:#333
```

---

## 附录：模块依赖关系

```mermaid
graph BT
    API[rpc-api]
    REG[rpc-registry]
    CORE[rpc-core]
    SPRING[spring-boot-starter]
    AI[ai-forecasting-service]
    
    REG --> API
    CORE --> REG
    CORE --> API
    SPRING --> CORE
    CORE -.->|HTTP| AI
    
    EP[example-provider] --> SPRING
    EC[example-consumer] --> SPRING
```
