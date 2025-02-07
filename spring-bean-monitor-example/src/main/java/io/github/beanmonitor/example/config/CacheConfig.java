package io.github.beanmonitor.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 模拟耗时的缓存组件初始化
 */
@Configuration
public class CacheConfig {

    /**
     * 缓存管理器
     * 模拟复杂的缓存系统初始化
     */
    @Bean
    public CacheManager cacheManager() throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(800);
        return new CacheManager();
    }

    public static class CacheManager {
    }
} 