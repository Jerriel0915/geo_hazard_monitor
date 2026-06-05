package com.zwei.log.task;

import com.zwei.log.infrastructure.config.LogModuleProperties;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.LogStreamCheckpointMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日志清理任务
 *
 * @author zwei
 */
@Component("logCleanupTask")
public class LogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupTask.class);

    private final OperationLogMapper operationLogMapper;
    private final AuthLogMapper authLogMapper;
    private final RuntimeLogMapper runtimeLogMapper;
    private final LogStreamCheckpointMapper checkpointMapper;
    private final LogModuleProperties properties;

    public LogCleanupTask(OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper,
        LogStreamCheckpointMapper checkpointMapper,
        LogModuleProperties properties) {
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
        this.checkpointMapper = checkpointMapper;
        this.properties = properties;
    }

    public void cleanExpiredLogs() {
        int batchSize = properties.getCleanupBatchSize();
        Date logCutoffTime = toDate(LocalDateTime.now().minusDays(properties.getCleanupRetentionDays()));
        Date checkpointCutoffTime = toDate(LocalDateTime.now().minusDays(properties.getCleanupCheckpointRetentionDays()));

        int operationDeleted = deleteInBatches(limit -> operationLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int authDeleted = deleteInBatches(limit -> authLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int runtimeDeleted = deleteInBatches(limit -> runtimeLogMapper.deleteBefore(logCutoffTime, limit), batchSize);
        int checkpointDeleted = deleteInBatches(limit -> checkpointMapper.deleteBefore(checkpointCutoffTime, limit), batchSize);

        log.info("日志清理完成: operation={}, auth={}, runtime={}, checkpoint={}, retentionDays={}, checkpointRetentionDays={}, batchSize={}",
            operationDeleted, authDeleted, runtimeDeleted, checkpointDeleted, properties.getCleanupRetentionDays(),
            properties.getCleanupCheckpointRetentionDays(), batchSize);
    }

    private int deleteInBatches(DeleteExecutor executor, int batchSize) {
        int total = 0;
        while (true) {
            int deleted = executor.delete(batchSize);
            total += deleted;
            if (deleted < batchSize) {
                return total;
            }
        }
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @FunctionalInterface
    private interface DeleteExecutor {
        int delete(int limit);
    }
}
