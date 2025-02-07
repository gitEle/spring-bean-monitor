package io.github.beanmonitor.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试项目启动类
 * 用于演示 Bean 初始化监控功能
 */
@SpringBootApplication
public class SpringBeanMonitorAgentExample {
    public static void main(String[] args) {
        SpringApplication.run(SpringBeanMonitorAgentExample.class, args);
    }
}
