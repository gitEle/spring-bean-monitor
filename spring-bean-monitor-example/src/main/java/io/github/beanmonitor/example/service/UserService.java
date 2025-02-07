package io.github.beanmonitor.example.service;

import io.github.beanmonitor.example.config.DatabaseConfig.EntityManagerFactory;
import org.springframework.stereotype.Service;

/**
 * 用户服务
 * 模拟复杂的业务服务初始化
 */
@Service
public class UserService {
    private final EntityManagerFactory entityManagerFactory;

    public UserService(EntityManagerFactory entityManagerFactory) throws InterruptedException {
        this.entityManagerFactory = entityManagerFactory;
        // 模拟服务初始化耗时
        Thread.sleep(1000);
    }
} 