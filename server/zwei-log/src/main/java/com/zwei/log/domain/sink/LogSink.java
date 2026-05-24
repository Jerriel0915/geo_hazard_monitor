package com.zwei.log.domain.sink;

import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;

/**
 * 日志持久化接入点
 *
 * @author zwei
 */
public interface LogSink {

    boolean supports(LogType logType);

    void persist(AbstractLogRecord record);
}
