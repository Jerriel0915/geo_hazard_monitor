package com.zwei.iot.timeseries.service;

import com.zwei.iot.timeseries.config.MonitorIngestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PendingRecoveryService — PEL 超时消息回收")
class PendingRecoveryServiceTest {

    @Mock private RedisTemplate<Object, Object> redisTemplate;
    @Mock private StreamOperations<Object, Object, Object> streamOperations;
    @Mock private RedisConnection redisConnection;
    private MonitorIngestProperties properties;
    private PendingRecoveryService service;

    @BeforeEach
    void setUp() {
        properties = new MonitorIngestProperties();
        properties.setStreamKey("stream:monitor:ingest");
        properties.setConsumerGroup("test-group");
        properties.setConsumerName("test-consumer");
        properties.setPelRecoverIdleMs(60_000L);
        service = new PendingRecoveryService(redisTemplate, properties);
    }

    @Nested
    @DisplayName("decodeBody — fields 解码")
    class DecodeBody {

        @Test
        @DisplayName("标准 k-v 对解码并自动设置 retryCount=0")
        void decodesKeyValuePairs() {
            List<Object> fields = new ArrayList<>();
            fields.add("payload".getBytes(StandardCharsets.UTF_8));
            fields.add("{\"val\":1}".getBytes(StandardCharsets.UTF_8));
            fields.add("deviceCode".getBytes(StandardCharsets.UTF_8));
            fields.add("DEV-001".getBytes(StandardCharsets.UTF_8));

            Map<String, String> body = PendingRecoveryService.decodeBody(fields);

            assertThat(body).containsEntry("payload", "{\"val\":1}");
            assertThat(body).containsEntry("deviceCode", "DEV-001");
            assertThat(body).containsEntry("retryCount", "0");
        }

        @Test
        @DisplayName("null value 解码为空字符串")
        void nullValueBecomesEmptyString() {
            List<Object> fields = new ArrayList<>();
            fields.add("key".getBytes(StandardCharsets.UTF_8));
            fields.add(null);

            Map<String, String> body = PendingRecoveryService.decodeBody(fields);

            assertThat(body).containsEntry("key", "");
        }

        @Test
        @DisplayName("空 fields 返回仅含 retryCount 的 Map")
        void emptyFieldsReturnsOnlyRetryCount() {
            Map<String, String> body = PendingRecoveryService.decodeBody(List.of());
            assertThat(body).hasSize(1).containsEntry("retryCount", "0");
        }

        @Test
        @DisplayName("奇数个 fields 忽略最后一个无 value 的 key")
        void oddFieldCountDropsLast() {
            List<Object> fields = new ArrayList<>();
            fields.add("payload".getBytes(StandardCharsets.UTF_8));
            fields.add("ok".getBytes(StandardCharsets.UTF_8));
            fields.add("orphan".getBytes(StandardCharsets.UTF_8)); // no value

            Map<String, String> body = PendingRecoveryService.decodeBody(fields);

            assertThat(body).containsEntry("payload", "ok");
            assertThat(body).doesNotContainKey("orphan");
        }
    }

    @Nested
    @DisplayName("recover — 无待回收消息")
    class RecoverEmpty {

        @Test
        @DisplayName("XAUTOCLAIM 返回空列表时 recover 返回 0")
        void emptyXautoclaimReturnsZero() {
            when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(0L);

            int recovered = service.recover();

            assertThat(recovered).isZero();
        }
    }

    @Nested
    @DisplayName("serialize — 辅助方法")
    class Serialize {

        @Test
        @DisplayName("字符串转 UTF-8 字节数组")
        void utf8Encoding() {
            byte[] result = PendingRecoveryService.serialize("hello");
            assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo("hello");
        }
    }
}
