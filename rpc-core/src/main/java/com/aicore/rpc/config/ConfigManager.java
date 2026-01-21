package com.aicore.rpc.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author YourName
 * @date 2023-10-28
 *
 * 配置管理器。
 *
 * 职责:
 * 1. 加载 classpath 下的 rpc.properties 配置文件。
 * 2. 提供静态方法来获取各项配置值。
 * 3. 实现配置的单例加载，避免重复读取文件。
 * 4. 为配置项提供默认值，增强健壮性。
 */
public class ConfigManager {

    private static final Properties properties = new Properties();

    static {
        // 在静态代码块中加载配置文件，确保只加载一次
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("rpc.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find rpc.properties. Using default values.");
            } else {
                properties.load(input);
                System.out.println("Loaded configuration from rpc.properties");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            // 即使加载失败，也继续使用默认值，而不是让程序崩溃
        }
    }

    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer format for key '" + key + "'. Using default value: " + defaultValue);
            }
        }
        return defaultValue;
    }
}