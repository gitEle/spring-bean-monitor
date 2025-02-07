# Spring Bean Monitor Agent

一个基于字节码增强技术的 Spring Bean 初始化时间监控工具。通过 Java Agent 技术，在运行时对 Spring Bean 的创建过程进行监控，精确记录每个 Bean 的初始化时间，帮助分析和优化应用启动性能。

## 主要特点

- 使用字节码增强技术，无需修改应用代码
- 精确记录 Bean 初始化时间，包括总时间和自身时间
- 支持分析 Bean 之间的依赖关系
- 自动生成详细的初始化时间报告
- 零侵入性，不影响应用正常运行
- 线程安全设计，支持多线程环境
- 轻量级实现，性能开销小

## 实现原理

### 1. 字节码增强
- 使用 ASM 框架对 Spring 核心类进行字节码增强
- 主要增强以下类和方法：
  - `AbstractAutowireCapableBeanFactory.createBean`: 监控 Bean 的创建过程
  - `SpringApplication.run`: 检测应用启动完成时机

### 2. Bean 初始化时间收集
- 在 Bean 创建前后记录时间戳
- 使用 ThreadLocal 存储线程相关的 Bean 初始化栈
- 通过栈结构自动构建 Bean 之间的父子依赖关系
- 分别计算：
  - 总初始化时间（包含依赖初始化时间）
  - 自身初始化时间（不包含依赖初始化时间）

### 3. 数据存储
- 使用 ConcurrentHashMap 存储所有 Bean 的初始化信息
- 支持多线程并发访问和修改
- 保证数据的线程安全性

### 4. 报告生成
- 在应用启动完成后自动生成报告
- 包含以下信息：
  - 总初始化 Bean 数量
  - Top 10 耗时最长的 Bean（按总时间排序）
  - Top 10 耗时最长的 Bean（按自身时间排序）
  - Bean 的依赖关系展示

## 使用方法

### 1. 构建项目

```bash
mvn clean package
```

### 2. 配置 Java Agent

在应用启动时添加 JVM 参数：

```bash
java -javaagent:/path/to/spring-bean-monitor-agent.jar -jar your-application.jar
```

### 3. 查看监控报告

应用启动完成后，会自动生成类似下面的初始化时间报告：

```
=== Bean Initialization Report ===
Total initialized beans: 150

Top beans by total initialization time (including dependencies):
1. entityManagerFactory: 2500ms (self: 800ms)
   Dependencies:
   - dataSource: 1200ms
   - hibernateProperties: 500ms

2. webServerFactory: 1800ms (self: 1500ms)
   Dependencies:
   - sslProperties: 300ms

3. userService: 1000ms (self: 900ms)
   Dependencies:
   - userRepository: 100ms

Top beans by self initialization time (excluding dependencies):
1. webServerFactory: 1500ms
2. dataSource: 1200ms
3. userService: 900ms
```

## 最佳实践

1. 开发环境使用
   - 在本地开发环境使用，帮助发现初始化慢的 Bean
   - 优化 Bean 的初始化逻辑，提升启动性能

2. 测试环境使用
   - 在测试环境进行性能基准测试
   - 监控版本迭代对启动性能的影响

3. 生产环境使用
   - 建议仅在排查特定问题时使用
   - 注意日志输出可能带来的性能影响

## 注意事项

1. 性能影响
   - 虽然工具本身的性能开销很小，但会略微增加应用启动时间
   - 报告生成会产生一定的内存开销

2. 兼容性
   - 支持 Spring Framework 5.x 及以上版本
   - 支持 Spring Boot 2.x 及以上版本
   - 需要 JDK 8 或更高版本

3. 故障排除
   - 如果遇到类转换错误，检查目标类是否已被其他 Agent 修改
   - 确保 ASM 库版本与应用使用的其他 ASM 相关库兼容

