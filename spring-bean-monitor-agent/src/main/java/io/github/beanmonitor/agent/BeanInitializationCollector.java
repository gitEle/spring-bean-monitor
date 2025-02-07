package io.github.beanmonitor.agent;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Bean 初始化信息收集器
 * 该类负责收集和统计 Spring Bean 的初始化时间信息，包括总时间和自身时间
 * <p>
 * 主要功能：
 * 1. 记录每个 Bean 的初始化开始和结束时间
 * 2. 维护 Bean 之间的父子依赖关系
 * 3. 生成详细的初始化时间报告
 * 4. 支持多线程环境下的数据收集
 *
 * @author liuyangyang
 */
public class BeanInitializationCollector {
    // 存储所有 Bean 的初始化信息，使用 ConcurrentHashMap 保证线程安全
    private static final Map<String, BeanInitInfo> beanInitInfoMap = new ConcurrentHashMap<>();
    // 使用 ThreadLocal 存储每个线程正在初始化的 Bean 栈，用于构建依赖关系
    private static final ThreadLocal<Stack<BeanInitInfo>> initializingBeansStack = ThreadLocal.withInitial(
            Stack::new);
    // 标记应用是否已经启动完成
    private static volatile boolean applicationStarted = false;

    /**
     * 记录 Bean 初始化开始
     * 创建或获取 Bean 的初始化信息，记录开始时间，并建立父子依赖关系
     *
     * @param beanName Bean 名称
     */
    public static void recordStart(String beanName) {
        BeanInitInfo info = beanInitInfoMap.computeIfAbsent(
                beanName,
                k -> new BeanInitInfo(beanName)
        );
        info.startTime = System.currentTimeMillis();

        // 记录父子关系
        Stack<BeanInitInfo> stack = initializingBeansStack.get();
        if (!stack.isEmpty()) {
            BeanInitInfo parent = stack.peek();
            info.parent = parent;
            parent.children.add(info);
        }

        stack.push(info);
    }

    /**
     * 记录 Bean 初始化结束
     * 更新 Bean 的结束时间，计算总时间和自身时间（不包括依赖初始化时间）
     *
     * @param beanName Bean 名称
     */
    public static void recordEnd(String beanName) {
        Stack<BeanInitInfo> stack = initializingBeansStack.get();
        if (!stack.isEmpty() && stack.peek().beanName.equals(beanName)) {
            BeanInitInfo info = stack.pop();
            info.endTime = System.currentTimeMillis();
            info.totalDuration = info.endTime - info.startTime;

            // 计算实际初始化时间（不包括依赖的初始化时间）
            info.selfDuration = info.totalDuration;
            for (BeanInitInfo child : info.children) {
                info.selfDuration -= child.totalDuration;
            }
        }
    }

    /**
     * 标记应用启动完成
     * 当 Spring 应用启动完成时调用此方法，触发初始化报告的生成
     */
    public static void markApplicationStarted() {
        applicationStarted = true;
        generateReport();
    }

    /**
     * 生成初始化报告
     * 统计并输出 Bean 初始化时间的详细报告，包括：
     * 1. 总初始化 Bean 数量
     * 2. 按总初始化时间排序的 Top 10 Bean（包括依赖时间）
     * 3. 按自身初始化时间排序的 Top 10 Bean（不包括依赖时间）
     */
    private static void generateReport() {
        List<BeanInitInfo> sortedByTotal = beanInitInfoMap.values().stream()
                .filter(info -> info.totalDuration > 0)
                .sorted((a, b) -> Long.compare(b.totalDuration, a.totalDuration))
                .collect(Collectors.toList());

        List<BeanInitInfo> sortedBySelf = beanInitInfoMap.values().stream()
                .filter(info -> info.selfDuration > 0)
                .sorted((a, b) -> Long.compare(b.selfDuration, a.selfDuration))
                .collect(Collectors.toList());

        System.out.println("\n=== Bean Initialization Report ===");
        System.out.println("Total initialized beans: " + sortedByTotal.size());

        System.out.println("\nTop beans by total initialization time (including dependencies):");
        for (int i = 0; i < Math.min(10, sortedByTotal.size()); i++) {
            BeanInitInfo info = sortedByTotal.get(i);
            System.out.printf("%d. %s: %dms (self: %dms)%n",
                    i + 1, info.beanName, info.totalDuration, info.selfDuration
            );
            if (!info.children.isEmpty()) {
                System.out.println("   Dependencies:");
                for (BeanInitInfo child : info.children) {
                    System.out.printf("   - %s: %dms%n", child.beanName, child.totalDuration);
                }
            }
        }

        System.out.println("\nTop beans by self initialization time (excluding dependencies):");
        for (int i = 0; i < Math.min(10, sortedBySelf.size()); i++) {
            BeanInitInfo info = sortedBySelf.get(i);
            System.out.printf("%d. %s: %dms%n", i + 1, info.beanName, info.selfDuration);
        }

        System.out.println("\n=== End of Report ===\n");
    }

    /**
     * Bean 初始化信息内部类
     * 存储单个 Bean 的所有初始化相关信息
     */
    private static class BeanInitInfo {
        private final String beanName;        // Bean 的名称
        private long startTime;               // 初始化开始时间
        private long endTime;                 // 初始化结束时间
        private long totalDuration;           // 总初始化时间（包括依赖）
        private long selfDuration;            // 自身初始化时间（不包括依赖）
        private BeanInitInfo parent;          // 父 Bean（依赖于谁）
        private final Set<BeanInitInfo> children = new HashSet<>();  // 子 Bean 集合（被谁依赖）

        public BeanInitInfo(String beanName) {
            this.beanName = beanName;
        }
    }
} 