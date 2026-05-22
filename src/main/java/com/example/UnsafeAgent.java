package com.example;

import javassist.*;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;

public class UnsafeAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[UnsafeAgent] Agent loaded. Monitoring class definitions...");

        // Ensure the dump directory exists
        // Note: Using a relative path 'dumped_classes' for better compatibility, 
        // but you can change it to your specific path like "C:/Users/Library/Documents/hachimi/"
        String dumpDirPath = "dumped_classes";
        File dumpDir = new File(dumpDirPath);
        if (!dumpDir.exists()) {
            dumpDir.mkdirs();
        }

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                
                // Most reliable way to capture classes defined via Unsafe:
                // They all trigger this transform method.
                if (className == null || isTarget(className)) {
                    saveBytecode(className, classfileBuffer, dumpDirPath);
                }

                // If you specifically want to instrument sun.misc.Unsafe itself:
                if ("sun/misc/Unsafe".equals(className)) {
                    return instrumentUnsafe(classfileBuffer, dumpDirPath);
                }

                return null;
            }
        }, true);
    }

    private static boolean isTarget(String className) {
        // You can filter which classes to dump here
        return true; 
    }

    private static void saveBytecode(String className, byte[] bytes, String dumpPath) {
        try {
            String fileName = (className != null) ? className.replace('/', '.') : "DynamicClass_" + System.nanoTime();
            File file = new File(dumpPath, fileName + ".class");
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }
        } catch (Exception e) {
            // Ignore errors in dumping
        }
    }

    private static byte[] instrumentUnsafe(byte[] classfileBuffer, String dumpPath) {
        try {
            ClassPool pool = ClassPool.getDefault();
            // Use ByteArrayInputStream to load the current bytecode into Javassist
            CtClass cc = pool.makeClass(new ByteArrayInputStream(classfileBuffer));
            
            try {
                CtMethod m = cc.getDeclaredMethod("defineClass");
                
                // Injecting the user's logic
                // Note: This only works if defineClass is NOT a native method in your JVM.
                m.insertBefore("{ " +
                    "   String name = ($1 != null) ? $1 : \"Unsafe_\" + System.nanoTime();" +
                    "   java.io.FileOutputStream fos = new java.io.FileOutputStream(\"" + dumpPath + "/\" + name.replace('.', '_') + \".class\");" +
                    "   fos.write($2, $3, $4);" +
                    "   fos.close();" +
                    "}");
                
                System.out.println("[UnsafeAgent] Successfully instrumented sun.misc.Unsafe.defineClass");
                return cc.toBytecode();
            } catch (NotFoundException e) {
                // Method might not be found or name is different
            } catch (CannotCompileException e) {
                System.err.println("[UnsafeAgent] Cannot instrument Unsafe.defineClass (possibly a native method).");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
