package com.zwei.log.application.service;

import org.springframework.stereotype.Component;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.sink.FileLogWriter;

/**
 * 文件写入占位实现
 *
 * @author zwei
 */
@Component
public class NoopFileLogWriter implements FileLogWriter {

    @Override
    public void write(AbstractLogRecord record) {
        // 文件写入能力保留扩展点，本期不启用具体逻辑
    }
}
