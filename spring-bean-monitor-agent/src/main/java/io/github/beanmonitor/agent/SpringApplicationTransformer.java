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
 * Spring Application 转换器
 * 用于检测 Spring 应用启动完成的字节码转换器
 * <p>
 * 主要功能：
 * 1. 通过字节码增强监控 SpringApplication.run 方法
 * 2. 在应用启动完成时通知收集器生成报告
 * <p>
 * 实现原理：
 * 1. 使用 ASM 对 SpringApplication 类的 run 方法进行字节码增强
 * 2. 在方法结束时调用 BeanInitializationCollector.markApplicationStarted()
 *
 * @author liuyangyang
 */
public class SpringApplicationTransformer implements ClassFileTransformer {
    private static final Logger logger = LoggerFactory.getLogger(SpringApplicationTransformer.class);
    // 目标类的内部名称
    private static final String TARGET_CLASS = "org/springframework/boot/SpringApplication";
    // 需要增强的目标方法名
    private static final String TARGET_METHOD = "run";
    // 目标方法的方法描述符
    private static final String TARGET_METHOD_DESC = "([Ljava/lang/String;)Lorg/springframework/context/ConfigurableApplicationContext;";
    // 收集器类的内部名称
    private static final String COLLECTOR_CLASS = "io/github/beanmonitor/agent/BeanInitializationCollector";

    /**
     * 转换类字节码的核心方法
     * 仅对 SpringApplication 类的字节码进行转换，为其 run 方法添加监控代码
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
        if (className == null || !className.equals(TARGET_CLASS)) {
            return classfileBuffer;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new SpringApplicationClassVisitor(cw), ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            logger.error("Transform class {} failed", className, e);
            return classfileBuffer;
        }
    }

    /**
     * Spring Application 类访问器
     * 用于访问和转换 SpringApplication 类的字节码
     * 继承自 ASM 的 ClassVisitor，只关注 run 方法的转换
     */
    private static class SpringApplicationClassVisitor extends org.objectweb.asm.ClassVisitor {
        public SpringApplicationClassVisitor(ClassWriter cw) {
            super(Opcodes.ASM9, cw);
        }

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

            if (TARGET_METHOD.equals(name) && TARGET_METHOD_DESC.equals(descriptor)) {
                return new SpringApplicationMethodAdapter(mv, access, name, descriptor);
            }
            return mv;
        }
    }

    /**
     * Spring Application 方法适配器
     * 用于在 run 方法结束时注入监控代码
     * 继承自 AdviceAdapter，提供了方法退出时的回调机制
     */
    private static class SpringApplicationMethodAdapter extends AdviceAdapter {
        protected SpringApplicationMethodAdapter(
                org.objectweb.asm.MethodVisitor mv, int access,
                String name, String descriptor
        ) {
            super(Opcodes.ASM9, mv, access, name, descriptor);
        }

        @Override
        protected void onMethodExit(int opcode) {
            // 在 run 方法结束时调用收集器的 markApplicationStarted 方法
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    COLLECTOR_CLASS,
                    "markApplicationStarted",
                    "()V",
                    false
            );
        }
    }
} 