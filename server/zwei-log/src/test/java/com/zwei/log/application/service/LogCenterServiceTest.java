package com.zwei.log.application.service;

import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.zwei.log.domain.model.LogRuntimeRecord;
import com.zwei.log.domain.sink.LogStorageRouter;
import com.zwei.log.infrastructure.persistence.mysql.AuthLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.OperationLogMapper;
import com.zwei.log.infrastructure.persistence.mysql.RuntimeLogMapper;
import com.zwei.log.infrastructure.push.sse.LogStreamPublisher;
import com.zwei.log.infrastructure.sequence.EventIdGenerator;

class LogCenterServiceTest {

    @Test
    void shouldFillRuntimeRecordDefaultsBeforePublish() {
        EventIdGenerator eventIdGenerator = new EventIdGenerator();
        LogStorageRouter router = Mockito.mock(LogStorageRouter.class);
        LogStreamPublisher publisher = Mockito.mock(LogStreamPublisher.class);
        OperationLogMapper operationLogMapper = Mockito.mock(OperationLogMapper.class);
        AuthLogMapper authLogMapper = Mockito.mock(AuthLogMapper.class);
        RuntimeLogMapper runtimeLogMapper = Mockito.mock(RuntimeLogMapper.class);
        Mockito.when(publisher.getActiveCount()).thenReturn(0);

        LogCenterService service = new LogCenterService(
            eventIdGenerator,
            router,
            publisher,
            operationLogMapper,
            authLogMapper,
            runtimeLogMapper
        );

        LogRuntimeRecord record = new LogRuntimeRecord();
        record.setMessage("runtime failure");

        service.publishRuntime(record);

        Assertions.assertNotNull(record.getEventId());
        Assertions.assertNotNull(record.getOccurredAt());
        Assertions.assertEquals("runtime failure", record.getMessageDigest());
        Mockito.verify(router).route(record);
        Mockito.verify(publisher).publish(record);
    }
}
