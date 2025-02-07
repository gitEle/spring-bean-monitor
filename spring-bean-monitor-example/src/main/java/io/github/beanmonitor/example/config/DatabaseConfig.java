package io.github.beanmonitor.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库配置类
 * 模拟耗时的数据库相关组件初始化
 */
@Configuration
public class DatabaseConfig {

    /**
     * 数据源配置
     * 模拟耗时的数据库连接池初始化
     */
    @Bean
    public DataSource dataSource() throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(1500);
        return new DataSource();
    }

    /**
     * JPA 实体管理器工厂
     * 依赖于 dataSource，模拟复杂的 ORM 配置
     */
    @Bean
    public EntityManagerFactory entityManagerFactory(DataSource dataSource) throws InterruptedException {
        // 模拟耗时操作
        Thread.sleep(2000);
        return new EntityManagerFactory(dataSource);
    }

    // 内部类，模拟实际组件
    public static class DataSource {
    }

    public static class EntityManagerFactory {
        public EntityManagerFactory(DataSource dataSource) {
        }
    }
} 