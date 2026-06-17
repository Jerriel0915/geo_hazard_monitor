package com.zwei.iot.timeseries.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("LastMessageStore")
class LastMessageStoreTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private LastMessageStore store;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        store = new LastMessageStore(redis, new ObjectMapper());
    }

    @Test
    @DisplayName("get 命中: 返回反序列化后的 snapshot")
    void getHit() {
        String json = "{\"deviceCode\":\"D1\",\"sensorCode\":\"S1\",\"dataTime\":1700000000000,"
                + "\"properties\":{\"rainfall\":12.5}}";
        when(valueOps.get("monitor:last:1:S1")).thenReturn(json);

        ParsedMessageSnapshot snap = store.get(1L, "S1");

        assertThat(snap).isNotNull();
        assertThat(snap.deviceCode()).isEqualTo("D1");
        assertThat(snap.dataTime()).isEqualTo(1700000000000L);
        assertThat(snap.properties()).containsEntry("rainfall", 12.5);
    }

    @Test
    @DisplayName("get miss: 返回 null")
    void getMiss() {
        when(valueOps.get(anyString())).thenReturn(null);
        assertThat(store.get(1L, "S1")).isNull();
    }

    @Test
    @DisplayName("get Redis 异常: 返回 null, 不抛")
    void getRedisFailure() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        assertThat(store.get(1L, "S1")).isNull();
    }

    @Test
    @DisplayName("put: 写入 JSON 并设置 TTL")
    void put() {
        ParsedMessageSnapshot snap = new ParsedMessageSnapshot(
                "D1", "S1", 1700000000000L, Map.of("rainfall", 12.5));

        store.put(1L, "S1", snap);

        verify(valueOps).set(eq("monitor:last:1:S1"), contains("\"rainfall\":12.5"),
                any(Duration.class));
    }

    @Test
    @DisplayName("put Redis 异常: 仅吞异常不抛")
    void putRedisFailure() {
        doThrow(new RuntimeException("redis down"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));

        ParsedMessageSnapshot snap = new ParsedMessageSnapshot(
                "D1", "S1", 1L, Map.of());

        // 不抛异常即视为通过
        store.put(1L, "S1", snap);
        verify(valueOps).set(anyString(), anyString(), any(Duration.class));
    }
}
