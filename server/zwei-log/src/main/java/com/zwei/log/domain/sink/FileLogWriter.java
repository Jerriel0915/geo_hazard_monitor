package com.zwei.log.domain.sink;

import com.zwei.log.domain.model.AbstractLogRecord;

/**
 * 文件日志写入器扩展点
 *
 * @author zwei
 */
public interface FileLogWriter {

    void write(AbstractLogRecord record);
}
