package io.github.beanmonitor.example.service;

import io.github.beanmonitor.example.config.CacheConfig.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 带缓存的用户服务
 * 组合了用户服务和缓存功能
 */
@Service
public class CacheableUserService {
    private final UserService userService;
    private final CacheManager cacheManager;

    public CacheableUserService(UserService userService, CacheManager cacheManager) throws InterruptedException {
        this.userService = userService;
        this.cacheManager = cacheManager;
        // 模拟复杂的缓存配置初始化
        Thread.sleep(500);
    }
} 