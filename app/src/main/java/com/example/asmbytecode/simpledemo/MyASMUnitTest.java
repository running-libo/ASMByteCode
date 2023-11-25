package com.example.asmbytecode.simpledemo;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyASMUnitTest {

    public void test() {
        try {
            File file = new File("src/main/java/com/example/asmbytecode/simpledemo/InjectTest.class");
            FileInputStream fis = new FileInputStream(file);
            //将class文件转成流
            ClassReader cr = new ClassReader(fis);
            //ClassWriter.COMPUTE_FRAMES 参数意义: 自动计算栈帧 和 局部变量表的大小
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);

            //执行分析
            cr.accept(new MyClassVisitor(Opcodes.ASM5, cw), ClassWriter.COMPUTE_FRAMES);

            System.out.println("Success!");

            //执行了插桩之后的字节码数据输出
            byte[] bytes = cw.toByteArray();
            FileOutputStream fos = new FileOutputStream("src/main/java/com/example/asmbytecode/simpledemo/InjectTest2.class");
            fos.write(bytes);
            fos.close();

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static class MyClassVisitor extends ClassVisitor {

        public MyClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            //类似于动态代理的机制，会将执行的方法进行回调，然后在方法执行之前和之后做操作
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MyMethodVisitor(api, methodVisitor, access, name, descriptor);
        }

    }

    static class MyMethodVisitor extends AdviceAdapter {
        private int startTimeId = -1;
        /**
         * 用变量区分方法是否需要执行插桩
         */
        boolean inject = false;
        private String methodName = null;

        protected MyMethodVisitor(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor);
            methodName = name;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            //descriptor为方法的注解类型 行如： Lcom/example/bytecodeProject/ASMTest
            //如果方法的注解为ASMTest，则执行插桩代码
            if (descriptor.equals("Lcom/example/asmbytecode/simpledemo/ASMTest")) {
                inject = true;
            }
            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        protected void onMethodEnter() {  //代码插入到方法头部
            super.onMethodEnter();

            if (!inject) {
                return;
            }

            //在Java kotlin中写代码直接写，但是ASM写代码有最大区别，就是需要用方法签名的格式来写。

            //long l = System.currentTimeMillis();
            //要写如上一行代码的字节码，需要执行一个静态方法，，类是System,方法名是currentTimeMillis，所以有如下代码：
            startTimeId = newLocal(Type.LONG_TYPE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitIntInsn(LSTORE, startTimeId);
        }

        @Override
        protected void onMethodExit(int opcode) { //代码插入到方法结尾
            super.onMethodExit(opcode);

            if (!inject) {
                return;
            }

            int durationId = newLocal(Type.LONG_TYPE);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LLOAD, startTimeId);
            mv.visitInsn(LSUB);
            mv.visitVarInsn(LSTORE, durationId);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("The cost time of " + methodName + "() is ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(LLOAD, durationId);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(" ms");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

        }
    }
}
