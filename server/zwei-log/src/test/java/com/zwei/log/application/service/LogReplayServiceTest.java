package com.zwei.log.application.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.LogAuthRecord;
import com.zwei.log.domain.model.LogOperationRecord;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.domain.model.LogStreamCheckpoint;
import com.zwei.log.infrastructure.config.LogModuleProperties;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.LogStreamCheckpointMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;

class LogReplayServiceTest {

    @Test
    void shouldPreferLastEventIdHeaderWhenProvided() {
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        LogStreamCheckpointMapper checkpointMapper = Mockito.mock(LogStreamCheckpointMapper.class);
        LogReplayService service = new LogReplayService(operationLogMapper, authLogMapper, runtimeLogMapper, checkpointMapper, createProperties());

        Long resumeId = service.resolveResumeEventId("resume-key", Set.of(LogType.AUTH), 123L);

        Assertions.assertEquals(123L, resumeId);
        Mockito.verifyNoInteractions(checkpointMapper);
    }

    @Test
    void shouldLoadAndSortReplayRecordsAcrossTypes() {
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        LogStreamCheckpointMapper checkpointMapper = Mockito.mock(LogStreamCheckpointMapper.class);
        LogReplayService service = new LogReplayService(operationLogMapper, authLogMapper, runtimeLogMapper, checkpointMapper, createProperties());

        LogOperationRecord operationRecord = new LogOperationRecord();
        operationRecord.setEventId(30L);
        LogAuthRecord authRecord = new LogAuthRecord();
        authRecord.setEventId(10L);
        LogRuntimeRecord runtimeRecord = new LogRuntimeRecord();
        runtimeRecord.setEventId(20L);
        Mockito.when(operationLogMapper.selectAfterEventId(1L, 200)).thenReturn(List.of(operationRecord));
        Mockito.when(authLogMapper.selectAfterEventId(1L, 200)).thenReturn(List.of(authRecord));
        Mockito.when(runtimeLogMapper.selectAfterEventId(1L, 200)).thenReturn(List.of(runtimeRecord));

        List<?> records = service.loadReplayRecords(Set.of(LogType.OPERATION, LogType.AUTH, LogType.RUNTIME), 1L);

        Assertions.assertEquals(3, records.size());
        Assertions.assertEquals(10L, ((LogAuthRecord) records.get(0)).getEventId());
        Assertions.assertEquals(20L, ((LogRuntimeRecord) records.get(1)).getEventId());
        Assertions.assertEquals(30L, ((LogOperationRecord) records.get(2)).getEventId());
    }

    @Test
    void shouldUseCheckpointWhenHeaderMissing() {
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        LogStreamCheckpointMapper checkpointMapper = Mockito.mock(LogStreamCheckpointMapper.class);
        LogReplayService service = new LogReplayService(operationLogMapper, authLogMapper, runtimeLogMapper, checkpointMapper, createProperties());

        LogStreamCheckpoint checkpoint = new LogStreamCheckpoint();
        checkpoint.setLastEventId(456L);
        Mockito.when(checkpointMapper.selectBySubscriberAndTypes("resume-key", List.of("AUTH"))).thenReturn(List.of(checkpoint));

        Long resumeId = service.resolveResumeEventId("resume-key", Set.of(LogType.AUTH), null);

        Assertions.assertEquals(456L, resumeId);
    }

    @Test
    void shouldFlushCheckpointByIntervalAndOnDemand() {
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        LogStreamCheckpointMapper checkpointMapper = Mockito.mock(LogStreamCheckpointMapper.class);
        LogModuleProperties properties = createProperties();
        properties.setSseCheckpointFlushIntervalMs(100000L);
        LogReplayService service = new LogReplayService(operationLogMapper, authLogMapper, runtimeLogMapper, checkpointMapper, properties);

        service.saveCheckpoint("resume-key", LogType.OPERATION, 100L);
        Mockito.verifyNoInteractions(checkpointMapper);

        Assertions.assertEquals(100L, service.resolveResumeEventId("resume-key", Set.of(LogType.OPERATION), null));

        service.flushPendingCheckpoints("resume-key");

        Mockito.verify(checkpointMapper).upsert(Mockito.argThat(checkpoint ->
            "resume-key".equals(checkpoint.getSubscriberKey())
                && "OPERATION".equals(checkpoint.getLogType())
                && Long.valueOf(100L).equals(checkpoint.getLastEventId())));
    }

    private LogModuleProperties createProperties() {
        LogModuleProperties properties = new LogModuleProperties();
        properties.setSseCheckpointFlushIntervalMs(5000L);
        return properties;
    }
}
