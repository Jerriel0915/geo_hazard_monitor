package com.zwei.log.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.zwei.common.utils.StringUtils;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.AbstractLogRecord;
import com.zwei.log.domain.model.LogStreamCheckpoint;
import com.zwei.log.infrastructure.config.LogModuleProperties;
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
    private final LogModuleProperties properties;
    private final ConcurrentHashMap<String, PendingCheckpoint> pendingCheckpoints = new ConcurrentHashMap<>();

    public LogReplayService(OperationLogMapper operationLogMapper,
        AuthLogMapper authLogMapper,
        RuntimeLogMapper runtimeLogMapper,
        LogStreamCheckpointMapper logStreamCheckpointMapper,
        LogModuleProperties properties) {
        this.operationLogMapper = operationLogMapper;
        this.authLogMapper = authLogMapper;
        this.runtimeLogMapper = runtimeLogMapper;
        this.logStreamCheckpointMapper = logStreamCheckpointMapper;
        this.properties = properties;
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
        Long pendingResumeId = types.stream()
            .map(type -> pendingCheckpoints.get(buildCheckpointKey(subscriberKey, type)))
            .filter(item -> item != null && item.lastEventId() != null && item.lastEventId() > 0)
            .map(PendingCheckpoint::lastEventId)
            .min(Long::compareTo)
            .orElse(null);
        if (pendingResumeId != null) {
            return pendingResumeId;
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
        String logTypeName = logType.name();
        String checkpointKey = buildCheckpointKey(subscriberKey, logTypeName);
        long now = System.currentTimeMillis();
        long flushIntervalMs = properties.getSseCheckpointFlushIntervalMs();
        PendingCheckpoint pendingCheckpoint = pendingCheckpoints.compute(checkpointKey, (key, existing) -> {
            long lastFlushTime = existing == null ? now : existing.lastFlushTimeMs();
            return new PendingCheckpoint(subscriberKey, logTypeName, lastEventId, lastFlushTime, true);
        });
        if (flushIntervalMs == 0L || now - pendingCheckpoint.lastFlushTimeMs() >= flushIntervalMs) {
            flushCheckpoint(checkpointKey, pendingCheckpoint, now);
        }
    }

    public void flushPendingCheckpoints(String subscriberKey) {
        if (StringUtils.isEmpty(subscriberKey)) {
            return;
        }
        long now = System.currentTimeMillis();
        pendingCheckpoints.forEach((key, checkpoint) -> {
            if (subscriberKey.equals(checkpoint.subscriberKey()) && checkpoint.dirty()) {
                flushCheckpoint(key, checkpoint, now);
            }
        });
    }

    private void flushCheckpoint(String checkpointKey, PendingCheckpoint pendingCheckpoint, long flushTimeMs) {
        LogStreamCheckpoint checkpoint = new LogStreamCheckpoint();
        checkpoint.setSubscriberKey(pendingCheckpoint.subscriberKey());
        checkpoint.setLastEventId(pendingCheckpoint.lastEventId());
        checkpoint.setLogType(pendingCheckpoint.logType());
        logStreamCheckpointMapper.upsert(checkpoint);
        pendingCheckpoints.computeIfPresent(checkpointKey, (key, existing) -> {
            if (!existing.lastEventId().equals(checkpoint.getLastEventId())) {
                return existing;
            }
            return new PendingCheckpoint(existing.subscriberKey(), existing.logType(), existing.lastEventId(), flushTimeMs, false);
        });
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

    private String buildCheckpointKey(String subscriberKey, String logType) {
        return subscriberKey + "::" + logType;
    }

    private record PendingCheckpoint(String subscriberKey, String logType, Long lastEventId, long lastFlushTimeMs, boolean dirty) {
    }
}
