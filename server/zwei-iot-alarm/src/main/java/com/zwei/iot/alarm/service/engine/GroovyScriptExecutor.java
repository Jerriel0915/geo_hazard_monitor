package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.config.AlarmProperties;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Groovy 脚本执行器 — 用于综合告警策略的脚本评估。
 * <p>
 * 使用共享线程池 + 超时控制保证执行安全。脚本应返回 int 值表示告警等级 (1-4)，
 * 返回 0 或不返回表示无告警。
 *
 * @author zwei
 */
@Component
public class GroovyScriptExecutor {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptExecutor.class);

    private static final String[] FORBIDDEN_KEYWORDS = {
            "System.exit", "Runtime.getRuntime", "ProcessBuilder",
            "exec(", "Class.forName", "getClassLoader",
            "File(", "FileInputStream", "FileOutputStream",
            "Thread.sleep", "Thread.start",
            "System.getProperty", "System.setProperty"
    };

    private final AlarmProperties properties;
    private final ExecutorService executor;

    public GroovyScriptExecutor(AlarmProperties properties) {
        this.properties = properties;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "groovy-eval");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdownNow();
    }

    public Integer execute(String scriptContent, Map<String, Object> variables) {
        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            return null;
        }
        if (!isSafeScript(scriptContent)) {
            log.warn("Groovy脚本包含不安全代码，已拒绝执行");
            return null;
        }

        Future<Integer> future = executor.submit(() -> {
            try {
                GroovyShell shell = new GroovyShell();
                Binding binding = new Binding();
                if (variables != null) {
                    variables.forEach(binding::setVariable);
                }
                shell.setProperty("binding", binding);

                Object result = shell.evaluate(scriptContent);
                if (result == null) return null;
                if (result instanceof Number) {
                    int level = ((Number) result).intValue();
                    return (level >= 1 && level <= 4) ? level : null;
                }
                return null;
            } catch (Exception e) {
                log.error("Groovy脚本执行异常: {}", e.getMessage());
                return null;
            }
        });

        try {
            return future.get(properties.getGroovyTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Groovy脚本执行超时 ({}s)", properties.getGroovyTimeoutSeconds());
            future.cancel(true);
            return null;
        } catch (Exception e) {
            log.error("Groovy脚本执行中断", e);
            return null;
        }
    }

    private boolean isSafeScript(String script) {
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (script.contains(keyword)) {
                return false;
            }
        }
        return true;
    }
}
