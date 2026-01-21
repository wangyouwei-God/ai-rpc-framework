package com.aicore.rpc.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

/**
 * Annotation to mark a class as an RPC service implementation.
 * The framework will automatically register the service with the registry.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;AiRpcService
 *     public class HelloServiceImpl implements HelloService {
 *         @Override
 *         public String sayHello(String name) {
 *             return "Hello, " + name;
 *         }
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface AiRpcService {

    /**
     * Service interface class. If not specified, the first interface implemented by
     * the class will be used.
     */
    Class<?> interfaceClass() default void.class;

    /**
     * Service version for multi-version support.
     */
    String version() default "";

    /**
     * Service group for service isolation.
     */
    String group() default "";

    /**
     * Service weight for load balancing. Higher weight means more traffic.
     */
    int weight() default 100;
}
