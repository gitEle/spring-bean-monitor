package io.github.beanmonitor.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * Bean 监控 Agent 入口类
 * 该类是 Java Agent 的入口点，负责注册所有的字节码转换器
 * <p>
 * 主要功能：
 * 1. 初始化并注册 Bean 初始化监控转换器
 * 2. 初始化并注册 Spring 应用监控转换器
 * 3. 配置日志记录器
 * <p>
 * 使用方法：
 * 在 JVM 启动参数中添加：-javaagent:/path/to/spring-bean-monitor-agent.jar
 *
 * @author liuyangyang
 */
public class BeanMonitorAgent {
    private static final Logger logger = LoggerFactory.getLogger(BeanMonitorAgent.class);

    /**
     * Java Agent 入口方法
     * 在 JVM 启动时被调用，负责初始化和注册所有监控组件
     *
     * @param args Java Agent 的启动参数
     * @param inst JVM 提供的 Instrumentation 实例，用于注册字节码转换器
     */
    public static void premain(String args, Instrumentation inst) {
        logger.info("Bean Monitor Agent starting...");

        // 添加 Bean 初始化监控转换器
        inst.addTransformer(new BeanInitializationTransformer());

        // 添加 SpringApplication 监控转换器
        inst.addTransformer(new SpringApplicationTransformer());

        logger.info("Bean Monitor Agent started successfully");
    }
} 