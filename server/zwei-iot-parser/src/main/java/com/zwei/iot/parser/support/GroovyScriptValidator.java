package com.zwei.iot.parser.support;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import java.util.List;

/**
 * Groovy 脚本预编译校验 + 沙箱安全配置工厂。
 *
 * <h3>双重职责</h3>
 * <ol>
 *   <li><b>预编译校验</b> — {@link #validate(String)} 在策略保存前检查脚本语法，
 *       提前发现编译错误，避免运行时才发现脚本不可执行。</li>
 *   <li><b>安全配置工厂</b> — {@link #createSecureConfig()} 提供统一的 AST 级别沙箱配置，
 *       被 {@link com.zwei.iot.parser.engine.GroovyScriptEngine} 复用。</li>
 * </ol>
 *
 * <h3>沙箱分层防御</h3>
 * <p>Groovy 沙箱通过 {@link SecureASTCustomizer} 在编译期 AST 遍历阶段拦截危险调用，
 * 不依赖运行时权限检查，无法被反射或字符串拼接绕过：</p>
 * <ul>
 *   <li><b>Import 黑名单</b> — 禁止导入 {@code java.io.*} / {@code java.net.*} /
 *       {@code java.lang.reflect.*} 等包，阻止脚本直接引用危险类。</li>
 *   <li><b>Receiver 黑名单</b> — 禁止对 {@code System} / {@code Runtime} /
 *       {@code ProcessBuilder} / {@code Class} / {@code Thread} 等类型的静态方法调用，
 *       这是最核心的防线——即使脚本不 import 这些类，也无法通过全限定名调用。</li>
 *   <li><b>Star import 禁止</b> — 禁止通配符导入，避免意外引入危险类。</li>
 *   <li><b>Static import 禁止</b> — 禁止静态导入，防止绕过 import 黑名单。</li>
 * </ul>
 *
 * <p>注意：沙箱配置仅用于脚本编译阶段的安全拦截。运行时超时保护由
 * {@link com.zwei.iot.parser.engine.GroovyScriptEngine} 的 {@code Future.get(timeout)} 提供。
 */
public final class GroovyScriptValidator {

    private GroovyScriptValidator() {}

    /**
     * 预编译校验脚本语法及沙箱合规性。
     *
     * <p>在策略保存/更新时调用，提前发现编译错误。校验通过不代表运行时一定成功
     * （数据格式错误等运行时问题无法预检），但能保证脚本语法正确且不触发沙箱违规。
     *
     * @param scriptCode 完整 Groovy 脚本体
     * @return {@code null} 表示校验通过；否则返回中文错误描述
     */
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

    /**
     * 构建 AST 级别沙箱配置。
     *
     * <h3>拦截原理</h3>
     * <p>{@link SecureASTCustomizer} 在 Groovy 编译器的 AST 遍历阶段检查每个节点。
     * 与被 {@code SecurityManager} 废弃后的运行时权限检查不同，AST 拦截在编译期生效，
     * 无法被字符串拼接或反射绕过。
     *
     * <h3>黑名单设计原则</h3>
     * <table>
     *   <caption>各黑名单类别说明</caption>
     *   <tr><th>级别</th><th>机制</th><th>拦截对象</th><th>示例</th></tr>
     *   <tr><td>Import</td><td>{@code setDisallowedImports}</td><td>类/包导入语句</td><td>{@code import java.io.File}</td></tr>
     *   <tr><td>Star Import</td><td>{@code setDisallowedStarImports}</td><td>通配符导入</td><td>{@code import java.io.*}</td></tr>
     *   <tr><td>Static Import</td><td>{@code setDisallowedStaticImports}</td><td>静态方法导入</td><td>{@code import static java.lang.System.exit}</td></tr>
     *   <tr><td>Receiver</td><td>{@code setDisallowedReceivers}</td><td>方法调用的接收者类型</td><td>{@code System.exit(0)} — 即便不用 import 也能阻止</td></tr>
     * </table>
     *
     * <p>Receiver 黑名单是最后一道防线：即使脚本用全限定名
     * {@code java.lang.System.exit(0)}，Groovy 编译器在 AST 遍历时识别接收者类型
     * 为 {@code java.lang.System}，命中黑名单后直接拒绝编译。
     */
    public static CompilerConfiguration createSecureConfig() {
        CompilerConfiguration config = new CompilerConfiguration();
        SecureASTCustomizer secure = new SecureASTCustomizer();

        // ---- Import 黑名单 ----
        // 禁止 import 危险类/包，阻止脚本在源码层面直接引用
        secure.setDisallowedStarImports(List.of("*"));
        secure.setDisallowedImports(List.of(
            // 进程控制
            "java.lang.System", "java.lang.Runtime", "java.lang.ProcessBuilder",
            // 类加载 & 反射
            "java.lang.Thread", "java.lang.Class", "java.lang.ClassLoader",
            // 文件系统
            "java.io.File", "java.io.FileInputStream", "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            // 通配符屏蔽——按包封禁
            "java.lang.reflect.*", "java.lang.invoke.*", "java.net.*",
            "java.io.*", "java.nio.*", "javax.script.*",
            // Groovy 内部——防沙箱逃逸
            "groovy.lang.*", "org.codehaus.groovy.*"
        ));

        // 禁止静态导入——防止通过 import static 绕过 import 黑名单
        secure.setDisallowedStaticImports(List.of("*"));

        // ---- Receiver 黑名单（核心防线）----
        // 无论脚本如何引用目标类型（import / 全限定名 / 变量传递），
        // 只要方法调用的接收者命中此名单，编译阶段直接拒绝。
        secure.setDisallowedReceivers(List.of(
            // JVM 进程控制
            System.class.getName(), Runtime.class.getName(),
            ProcessBuilder.class.getName(),
            // 反射 & 类加载
            Class.class.getName(), Thread.class.getName(),
            java.io.File.class.getName(),
            // Groovy 引擎内部——防动态编译/执行逃逸
            "groovy.lang.GroovyShell", "groovy.lang.GroovyClassLoader",
            "groovy.lang.Script", "groovy.lang.Closure",
            "org.codehaus.groovy.runtime.InvokerHelper"
        ));

        config.addCompilationCustomizers(secure);
        return config;
    }
}
