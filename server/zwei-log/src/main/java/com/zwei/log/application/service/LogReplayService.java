package com.zwei.log.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.zwei.common.utils.StringUtils;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.model.LogStreamCheckpoint;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.LogStreamCheckpointMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;

/**
 * 日志流回放服务
 *
 * @author zwei
 */
@Service
public class LogReplayService {

    private static final int DEFAULT_REPLAY_LIMIT = 200;

    private final OperationLogMapper operationLogMapper;
    private final AuthLogMapper authLogMapper;
    private final RuntimeLogMapper runtimeLogMapper;
    private final LogStreamCheckpointMapper logStreamCheckpointMapper;

    public LogReplayService(OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper,
        LogStreamCheckpointMapper logStreamCheckpointMapper) {
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
        this.logStreamCheckpointMapper = logStreamCheckpointMapper;
    }

    public Long resolveResumeEventId(String subscriberKey, Set<LogType> logTypes, Long lastEventId) {
        if (lastEventId != null && lastEventId > 0) {
            return lastEventId;
        }
        if (StringUtils.isEmpty(subscriberKey)) {
            return null;
        }
        List<String> types = normalizeLogTypes(logTypes);
        if (types.isEmpty()) {
            return null;
        }
        List<LogStreamCheckpoint> checkpoints = logStreamCheckpointMapper.selectBySubscriberAndTypes(subscriberKey, types);
        return checkpoints.stream()
            .map(LogStreamCheckpoint::getLastEventId)
            .filter(item -> item != null && item > 0)
            .min(Long::compareTo)
            .orElse(null);
    }

    public List<AbstractLogRecord> loadReplayRecords(Set<LogType> logTypes, Long afterEventId) {
        if (afterEventId == null || afterEventId < 0) {
            return List.of();
        }
        List<AbstractLogRecord> records = new ArrayList<>();
        Set<LogType> normalizedTypes = normalizeTypes(logTypes);
        if (normalizedTypes.contains(LogType.OPERATION)) {
            records.addAll(operationLogMapper.selectAfterEventId(afterEventId, DEFAULT_REPLAY_LIMIT));
        }
        if (normalizedTypes.contains(LogType.AUTH)) {
            records.addAll(authLogMapper.selectAfterEventId(afterEventId, DEFAULT_REPLAY_LIMIT));
        }
        if (normalizedTypes.contains(LogType.RUNTIME)) {
            records.addAll(runtimeLogMapper.selectAfterEventId(afterEventId, DEFAULT_REPLAY_LIMIT));
        }
        return records.stream()
            .sorted(Comparator.comparing(AbstractLogRecord::getEventId))
            .limit(DEFAULT_REPLAY_LIMIT)
            .collect(Collectors.toList());
    }

    public void saveCheckpoint(String subscriberKey, LogType logType, Long lastEventId) {
        if (StringUtils.isEmpty(subscriberKey) || logType == null || lastEventId == null) {
            return;
        }
        LogStreamCheckpoint checkpoint = new LogStreamCheckpoint();
        checkpoint.setSubscriberKey(subscriberKey);
        checkpoint.setLastEventId(lastEventId);
        checkpoint.setLogType(logType.name());
        logStreamCheckpointMapper.upsert(checkpoint);
    }

    private Set<LogType> normalizeTypes(Set<LogType> logTypes) {
        if (logTypes == null || logTypes.isEmpty()) {
            return Set.of(LogType.OPERATION, LogType.AUTH, LogType.RUNTIME);
        }
        return logTypes;
    }

    private List<String> normalizeLogTypes(Set<LogType> logTypes) {
        return normalizeTypes(logTypes).stream().map(Enum::name).collect(Collectors.toList());
    }
}
