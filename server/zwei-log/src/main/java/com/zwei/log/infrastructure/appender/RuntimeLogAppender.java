package com.zwei.log.infrastructure.appender;

import java.util.Date;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.spring.SpringUtils;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.infrastructure.config.LogModuleProperties;

/**
 * 运行日志Appender
 *
 * @author zwei
 */
public class RuntimeLogAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject == null || eventObject.getLoggerName().startsWith("com.zwei.log")) {
            return;
        }
        if (!shouldCapture(eventObject)) {
            return;
        }
        LogCenterService logCenterService;
        try {
            logCenterService = SpringUtils.getBean(LogCenterService.class);
        } catch (Exception ex) {
            return;
        }

        LogRuntimeRecord record = new LogRuntimeRecord();
        record.setOccurredAt(new Date(eventObject.getTimeStamp()));
        record.setLevel(eventObject.getLevel().levelStr);
        record.setLoggerName(StringUtils.substring(eventObject.getLoggerName(), 0, 255));
        record.setThreadName(StringUtils.substring(eventObject.getThreadName(), 0, 128));
        record.setSourceApp("zwei-admin");
        record.setMessage(StringUtils.substring(eventObject.getFormattedMessage(), 0, 4000));
        record.setMessageDigest(StringUtils.substring(eventObject.getFormattedMessage(), 0, 512));
        if (eventObject.getThrowableProxy() != null) {
            record.setExceptionClass(eventObject.getThrowableProxy().getClassName());
            record.setStackTrace(StringUtils.substring(eventObject.getThrowableProxy().getMessage(), 0, 16000));
        }
        logCenterService.publishRuntime(record);
    }

    private boolean shouldCapture(ILoggingEvent eventObject) {
        try {
            LogModuleProperties properties = SpringUtils.getBean(LogModuleProperties.class);
            return properties.supportsRuntimeLevel(eventObject.getLevel().levelStr);
        } catch (Exception ignored) {
            return false;
        }
    }
}
