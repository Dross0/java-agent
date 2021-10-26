package ru.gaidamaka.java.agent.transform;

import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassTransformer implements ClassFileTransformer {
    private static final AtomicInteger count = new AtomicInteger();

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
        String classDotName = className.replace("/", ".");
        count.incrementAndGet();
        byte[] byteCode = classfileBuffer;

        if ("ru.gaidamaka.java.agent.TransactionProcessor".equals(classDotName)) {
            try {
                ClassPool pool = ClassPool.getDefault();
                pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
                CtClass ctClass = pool.get(classDotName);
                addTransactionNumber(ctClass);
                addTimeMeasure(ctClass);
                addCountClasses(ctClass);
                try {
                    byteCode = ctClass.toBytecode();
                } catch (IOException e) {
                    System.err.println("toByteCode error: " + e.getMessage());
                }
                ctClass.detach();
                return byteCode;
            } catch (NotFoundException | CannotCompileException e) {
                System.err.println(e.getMessage());
            }

        }
        return byteCode;
    }

    private void addCountClasses(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod method = ctClass.getDeclaredMethod("main");
        method.insertAfter(String.format("System.out.println(%d + \" loaded classes\");", count.get()));
    }

    private void addTimeMeasure(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod method = ctClass.getDeclaredMethod("main");
        ctClass.removeMethod(method);
        ctClass.addMethod(CtNewMethod.make("public static void main(String[] args) throws Exception{\n" +
                "        ru.gaidamaka.java.agent.TransactionProcessor tp = new  ru.gaidamaka.java.agent.TransactionProcessor();\n" +
                "long max = 0, sum = 0, min = Long.MAX_VALUE;" +
                "        for (int i = 0; i < 10; ++i) {\n" +
                "long start = System.currentTimeMillis();" +
                "            tp.processTransaction(i);\n" +
                "long time = System.currentTimeMillis() - start;" +
                "max = time > max ? time : max;" +
                "min = time < min ? time : min;" +
                "sum += time;" +
                "        }\n" +
                "System.out.println(\"min= \" + min);" +
                "System.out.println(\"max= \" + max);" +
                "System.out.println(\"avg= \" + sum / 10);" +
                "    }", ctClass));
    }

    public void addTransactionNumber(CtClass ctClass) throws NotFoundException, CannotCompileException {
        CtMethod method = ctClass.getDeclaredMethod("processTransaction");
        method.insertBefore("txNum = txNum + 99;");
    }
}
