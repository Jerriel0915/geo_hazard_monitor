package com.zwei.log.application.service;

import java.util.List;
import org.springframework.stereotype.Component;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.sink.LogSink;
import com.zwei.log.domain.sink.LogStorageRouter;

/**
 * 默认日志存储路由器
 *
 * @author zwei
 */
@Component
public class DefaultLogStorageRouter implements LogStorageRouter {

    private final List<LogSink> logSinks;

    public DefaultLogStorageRouter(List<LogSink> logSinks) {
        this.logSinks = logSinks;
    }

    @Override
    public void route(AbstractLogRecord record) {
        for (LogSink logSink : logSinks) {
            if (logSink.supports(record.getLogType())) {
                logSink.persist(record);
            }
        }
    }
}
