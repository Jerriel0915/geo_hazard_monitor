package com.zwei.log.domain.model;

import com.zwei.log.domain.enums.LogType;

/**
 * 运行日志
 *
 * @author zwei
 */
public class LogRuntimeRecord extends AbstractLogRecord {

    private static final long serialVersionUID = 1L;

    private String level;
    private String loggerName;
    private String threadName;
    private String bizModule;
    private String sourceApp;
    private String hostName;
    private String environment;
    private String message;
    private String messageDigest;
    private String exceptionClass;
    private String stackTrace;

    @Override
    public LogType getLogType() {
        return LogType.RUNTIME;
    }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }
    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }
    public String getBizModule() { return bizModule; }
    public void setBizModule(String bizModule) { this.bizModule = bizModule; }
    public String getSourceApp() { return sourceApp; }
    public void setSourceApp(String sourceApp) { this.sourceApp = sourceApp; }
    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getMessageDigest() { return messageDigest; }
    public void setMessageDigest(String messageDigest) { this.messageDigest = messageDigest; }
    public String getExceptionClass() { return exceptionClass; }
    public void setExceptionClass(String exceptionClass) { this.exceptionClass = exceptionClass; }
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
