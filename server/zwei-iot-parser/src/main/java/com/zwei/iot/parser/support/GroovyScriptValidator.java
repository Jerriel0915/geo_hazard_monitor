package com.zwei.iot.parser.support;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.List;

public final class GroovyScriptValidator {

    private GroovyScriptValidator() {}

    public static String validate(String scriptCode) {
        if (scriptCode == null || scriptCode.trim().isEmpty()) {
            return "脚本内容不能为空";
        }
        if (scriptCode.length() > 100_000) {
            return "脚本内容过长，超过100KB限制";
        }
        try {
            GroovyShell shell = new GroovyShell(createSecureConfig());
            shell.parse(scriptCode);
            return null;  // OK
        } catch (Exception e) {
            return "脚本编译失败: " + e.getMessage();
        }
    }

    private static CompilerConfiguration createSecureConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        SecureASTCustomizer secure = new SecureASTCustomizer();
        secure.setDisallowedStarImports(List.of("*"));
        secure.setDisallowedImports(List.of(
            "java.lang.System", "java.lang.Runtime", "java.lang.ProcessBuilder",
            "java.lang.Thread", "java.lang.Class", "java.lang.ClassLoader",
            "java.io.File", "java.io.FileInputStream", "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.lang.reflect.*", "java.lang.invoke.*", "java.net.*",
            "java.io.*", "java.nio.*", "javax.script.*",
            "groovy.lang.*", "org.codehaus.groovy.*"
        ));
        secure.setDisallowedStaticImports(List.of("*"));
        secure.setDisallowedReceivers(List.of(
            System.class.getName(), Runtime.class.getName(),
            ProcessBuilder.class.getName(), Class.class.getName(),
            Thread.class.getName(), java.io.File.class.getName(),
            "groovy.lang.GroovyShell", "groovy.lang.GroovyClassLoader",
            "groovy.lang.Script", "groovy.lang.Closure",
            "org.codehaus.groovy.runtime.InvokerHelper"
        ));
        config.addCompilationCustomizers(secure);
        return config;
    }
}
