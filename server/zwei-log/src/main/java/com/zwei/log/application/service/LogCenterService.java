package com.zwei.log.application.service;

import java.util.Date;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.zwei.common.utils.StringUtils;
import com.zwei.log.api.dto.AuthLogQuery;
import com.zwei.log.api.dto.OperationLogQuery;
import com.zwei.log.api.dto.RuntimeLogQuery;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.model.LogAuthRecord;
import com.zwei.log.domain.model.LogOperationRecord;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.domain.sink.LogStorageRouter;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;
import com.zwei.log.infrastructure.push.sse.LogStreamPublisher;
import com.zwei.log.infrastructure.sequence.EventIdGenerator;

/**
 * 日志中心服务
 *
 * @author zwei
 */
@Service
public class LogCenterService {

    private final EventIdGenerator eventIdGenerator;
    private final LogStorageRouter logStorageRouter;
    private final LogStreamPublisher logStreamPublisher;
    private final OperationLogMapper operationLogMapper;
    private final AuthLogMapper authLogMapper;
    private final RuntimeLogMapper runtimeLogMapper;

    public LogCenterService(EventIdGenerator eventIdGenerator,
        LogStorageRouter logStorageRouter,
        LogStreamPublisher logStreamPublisher,
        OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper) {
        this.eventIdGenerator = eventIdGenerator;
        this.logStorageRouter = logStorageRouter;
        this.logStreamPublisher = logStreamPublisher;
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
    }

    @Async("threadPoolTaskExecutor")
    public void publishOperation(LogOperationRecord record) {
        prepare(record);
        logStorageRouter.route(record);
        logStreamPublisher.publish(record);
    }

    @Async("threadPoolTaskExecutor")
    public void publishAuth(LogAuthRecord record) {
        prepare(record);
        logStorageRouter.route(record);
        logStreamPublisher.publish(record);
    }

    @Async("threadPoolTaskExecutor")
    public void publishRuntime(LogRuntimeRecord record) {
        prepare(record);
        if (StringUtils.isEmpty(record.getMessageDigest()) && StringUtils.isNotEmpty(record.getMessage())) {
            String digest = record.getMessage();
            record.setMessageDigest(StringUtils.substring(digest, 0, 512));
        }
        logStorageRouter.route(record);
        logStreamPublisher.publish(record);
    }

    public List<LogOperationRecord> queryOperation(OperationLogQuery query) {
        return operationLogMapper.selectPage(query);
    }

    public List<LogAuthRecord> queryAuth(AuthLogQuery query) {
        return authLogMapper.selectPage(query);
    }

    public List<LogRuntimeRecord> queryRuntime(RuntimeLogQuery query) {
        return runtimeLogMapper.selectPage(query);
    }

    public int getActiveStreamCount() {
        return logStreamPublisher.getActiveCount();
    }

    private void prepare(AbstractLogRecord record) {
        if (record.getEventId() == null) {
            record.setEventId(eventIdGenerator.nextId());
        }
        if (record.getOccurredAt() == null) {
            record.setOccurredAt(new Date());
        }
    }
}
