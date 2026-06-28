package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * 脚本日志工具，注入 Groovy 脚本的 {@code log} 变量。
 * 脚本内调用 log.info(msg) / log.warn(msg) / log.error(msg)，
 * 执行后日志序列化为 JSON 存入 alarm_strategy_execution_log.script_logs。
 */
public class ScriptLogger {

    private final Long strategyId;
    private final List<LogEntry> entries = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();

    public ScriptLogger(Long strategyId) {
        this.strategyId = strategyId;
    }

    public void info(String msg)  { add("INFO", msg); }
    public void warn(String msg)  { add("WARN", msg); }
    public void error(String msg) { add("ERROR", msg); }

    private void add(String level, String msg) {
        entries.add(new LogEntry(level, msg, System.currentTimeMillis() - startTime));
    }

    public String toJson() {
        if (entries.isEmpty()) return null;
        return JSON.toJSONString(entries);
    }

    public static class LogEntry {
        private final String level;
        private final String msg;
        private final long ts;

        public LogEntry(String level, String msg, long ts) {
            this.level = level;
            this.msg = msg;
            this.ts = ts;
        }

        public String getLevel() { return level; }
        public String getMsg() { return msg; }
        public long getTs() { return ts; }
    }
}
