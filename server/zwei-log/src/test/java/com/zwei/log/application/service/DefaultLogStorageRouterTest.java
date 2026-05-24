package com.zwei.log.application.service;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.zwei.log.domain.enums.LogType;
import com.zwei.log.domain.model.LogOperationRecord;
import com.zwei.log.domain.sink.LogSink;

class DefaultLogStorageRouterTest {

    @Test
    void shouldRouteRecordToSupportingSink() {
        LogSink supportedSink = Mockito.mock(LogSink.class);
        LogSink unsupportedSink = Mockito.mock(LogSink.class);
        Mockito.when(supportedSink.supports(LogType.OPERATION)).thenReturn(true);
        Mockito.when(unsupportedSink.supports(LogType.OPERATION)).thenReturn(false);

        DefaultLogStorageRouter router = new DefaultLogStorageRouter(List.of(unsupportedSink, supportedSink));
        LogOperationRecord record = new LogOperationRecord();

        router.route(record);

        Mockito.verify(supportedSink).persist(record);
        Mockito.verify(unsupportedSink, Mockito.never()).persist(record);
        Assertions.assertEquals(LogType.OPERATION, record.getLogType());
    }
}
