package com.zwei.log.infrastructure.persistence.mysql;

import org.springframework.stereotype.Component;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.model.LogAuthRecord;
import com.zwei.log.domain.model.LogOperationRecord;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.domain.sink.LogSink;

/**
 * MySQL日志持久化实现
 *
 * @author zwei
 */
@Component
public class MysqlLogSink implements LogSink {

    private final OperationLogMapper operationLogMapper;
    private final AuthLogMapper authLogMapper;
    private final RuntimeLogMapper runtimeLogMapper;

    public MysqlLogSink(OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper) {
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
    }

    @Override
    public boolean supports(LogType logType) {
        return logType != null;
    }

    @Override
    public void persist(AbstractLogRecord record) {
        if (record instanceof LogOperationRecord operationRecord) {
            operationLogMapper.insert(operationRecord);
            return;
        }
        if (record instanceof LogAuthRecord authRecord) {
            authLogMapper.insert(authRecord);
            return;
        }
        if (record instanceof LogRuntimeRecord runtimeRecord) {
            runtimeLogMapper.insert(runtimeRecord);
        }
    }
}
