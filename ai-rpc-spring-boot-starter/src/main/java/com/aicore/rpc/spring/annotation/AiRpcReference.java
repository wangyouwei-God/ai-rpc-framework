package com.aicore.rpc.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an interface as an RPC service reference.
 * The framework will create a proxy for the annotated field and handle RPC
 * calls automatically.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Service
 *     public class ConsumerService {
 *         @AiRpcReference
 *         private HelloService helloService;
 *
 *         public String doSomething() {
 *             return helloService.sayHello("World");
 *         }
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AiRpcReference {

    /**
     * Service version for multi-version support.
     */
    String version() default "";

    /**
     * Service group for service isolation.
     */
    String group() default "";

    /**
     * Load balancer type. Options: "random", "ai".
     */
    String loadbalancer() default "";

    /**
     * Request timeout in milliseconds. -1 means use global configuration.
     */
    int timeout() default -1;

    /**
     * Number of retry attempts on failure. -1 means use global configuration.
     */
    int retries() default -1;

    /**
     * Whether to check service availability on startup.
     */
    boolean check() default true;
}
