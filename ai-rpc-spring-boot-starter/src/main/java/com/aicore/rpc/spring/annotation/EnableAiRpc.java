package com.aicore.rpc.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.aicore.rpc.spring.config.AiRpcAutoConfiguration;

/**
 * Enable AI-RPC framework in Spring Boot application.
 * Add this annotation to the main application class.
 *
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;SpringBootApplication
 *     @EnableAiRpc
 *     public class Application {
 *         public static void main(String[] args) {
 *             SpringApplication.run(Application.class, args);
 *         }
 *     }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(AiRpcAutoConfiguration.class)
public @interface EnableAiRpc {

    /**
     * Base packages to scan for @AiRpcService annotations.
     * If not specified, the package of the annotated class will be used.
     */
    String[] basePackages() default {};
}
