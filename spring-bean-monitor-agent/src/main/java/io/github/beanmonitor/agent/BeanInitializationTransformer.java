package io.github.beanmonitor.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Bean 初始化字节码转换器
 * 该转换器用于监控 Spring Bean 的初始化过程，通过字节码增强技术在 Bean 创建前后添加监控代码
 * 主要监控目标为 AbstractAutowireCapableBeanFactory 类的 createBean 方法
 * <p>
 * 实现原理：
 * 1. 使用 ASM 在目标方法前后注入监控代码
 * 2. 通过 BeanInitializationCollector 收集 Bean 初始化的时间信息
 * 3. 支持记录 Bean 创建的开始和结束时间，用于分析 Bean 初始化性能
 * 4. 通过字节码增强在不修改源码的情况下实现监控
 *
 * @author liuyangyang
 */
public class BeanInitializationTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(BeanInitializationTransformer.class);
    // 目标类的内部名称，Spring Bean 工厂的核心实现类
    private static final String TARGET_CLASS = "org/springframework/beans/factory/support/AbstractAutowireCapableBeanFactory";
    // 需要增强的目标方法名，负责创建和初始化 Bean 的核心方法
    private static final String TARGET_METHOD = "createBean";
    // 目标方法的方法描述符，定义方法的参数和返回值类型
    private static final String TARGET_METHOD_DESC = "(Ljava/lang/String;Lorg/springframework/beans/factory/support/RootBeanDefinition;[Ljava/lang/Object;)Ljava/lang/Object;";
    // 收集器类的内部名称，用于收集 Bean 初始化时间信息
    private static final String COLLECTOR_CLASS = "io/github/beanmonitor/agent/BeanInitializationCollector";

    /**
     * 转换类字节码的核心方法
     * 仅对 AbstractAutowireCapableBeanFactory 类的字节码进行转换，为其 createBean 方法添加监控代码
     *
     * @param loader              类加载器
     * @param className           类名
     * @param classBeingRedefined 被重定义的类
     * @param protectionDomain    保护域
     * @param classfileBuffer     原始类字节码
     * @return 转换后的字节码，如果不需要转换则返回原始字节码
     */
    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer
    ) {
        // 只对目标类进行转换
        if (className == null || !className.equals(TARGET_CLASS)) {
            return classfileBuffer;
        }

        try {
            // 创建 ClassReader 读取字节码
            ClassReader cr = new ClassReader(classfileBuffer);
            // 创建 ClassWriter 用于生成新的字节码
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            // 使用自定义的 ClassVisitor 访问并转换字节码
            cr.accept(new BeanInitializationClassVisitor(cw), ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            logger.error("Transform class {} failed", className, e);
            return classfileBuffer;
        }
    }

    /**
     * Bean 初始化类访问器
     * 用于访问和转换 AbstractAutowireCapableBeanFactory 类的字节码
     * 继承自 ASM 的 ClassVisitor，只关注 createBean 方法的转换
     */
    private static class BeanInitializationClassVisitor extends org.objectweb.asm.ClassVisitor {
        public BeanInitializationClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM9, cw);
        }

        /**
         * 访问类的方法
         *
         * @param access     方法的访问标志
         * @param name       方法名
         * @param descriptor 方法描述符
         * @param signature  方法签名
         * @param exceptions 异常列表
         * @return 方法访问器
         */
        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(
                int access, String name, String descriptor,
                String signature, String[] exceptions
        ) {
            org.objectweb.asm.MethodVisitor mv = super.visitMethod(
                    access,
                    name,
                    descriptor,
                    signature,
                    exceptions
            );

            // 对目标方法进行转换
            if (TARGET_METHOD.equals(name) && TARGET_METHOD_DESC.equals(descriptor)) {
                return new BeanInitializationMethodAdapter(mv, access, name, descriptor);
            }
            return mv;
        }
    }

    /**
     * Bean 初始化方法适配器
     * 用于在 createBean 方法前后注入监控代码
     * 继承自 AdviceAdapter，提供了方法进入和退出时的回调机制
     * <p>
     * 实现功能：
     * 1. 在方法进入时记录 Bean 初始化开始时间
     * 2. 在方法退出时记录 Bean 初始化结束时间
     */
    private static class BeanInitializationMethodAdapter extends AdviceAdapter {
        protected BeanInitializationMethodAdapter(
                org.objectweb.asm.MethodVisitor mv, int access,
                String name, String descriptor
        ) {
            super(Opcodes.ASM9, mv, access, name, descriptor);
        }

        /**
         * 在方法开始处注入代码
         * 调用 BeanInitializationCollector.recordStart 记录 Bean 创建开始时间
         */
        @Override
        protected void onMethodEnter() {
            // 加载方法的第一个参数 beanName（在局部变量表中索引为1）
            mv.visitVarInsn(ALOAD, 1);
            // 调用收集器的 recordStart 方法
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    COLLECTOR_CLASS,
                    "recordStart",
                    "(Ljava/lang/String;)V",
                    false
            );
        }

        /**
         * 在方法结束处注入代码
         * 调用 BeanInitializationCollector.recordEnd 记录 Bean 创建结束时间
         *
         * @param opcode 方法结束的操作码
         */
        @Override
        protected void onMethodExit(int opcode) {
            // 加载方法的第一个参数 beanName（在局部变量表中索引为1）
            mv.visitVarInsn(ALOAD, 1);
            // 调用收集器的 recordEnd 方法
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    COLLECTOR_CLASS,
                    "recordEnd",
                    "(Ljava/lang/String;)V",
                    false
            );
        }
    }
} 