package com.zwei.iot.parser.domain;

import com.alibaba.fastjson2.JSON;
import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParsedMessage JSON serialization")
class ParsedMessageJsonTest {

    @Test
    @DisplayName("should round-trip ParsedMessage through JSON")
    void roundTrip() {
        ParsedMessage original = new ParsedMessage(
            "DEV001", "S001", "sys",
            1700000000000L, 1700000001000L,
            "abc123def456",
            List.of(
                new PropertyValue("rainfall", "小时降雨量", "mm", 25.5, 0),
                new PropertyValue("temperature", "温度", "C", 18.2, 0)
            )
        );

        String json = JSON.toJSONString(original);
        ParsedMessage restored = JSON.parseObject(json, ParsedMessage.class);

        assertThat(restored.deviceCode()).isEqualTo("DEV001");
        assertThat(restored.sensorCode()).isEqualTo("S001");
        assertThat(restored.sourceType()).isEqualTo("sys");
        assertThat(restored.dataTime()).isEqualTo(1700000000000L);
        assertThat(restored.receiveTime()).isEqualTo(1700000001000L);
        assertThat(restored.payloadHash()).isEqualTo("abc123def456");
        assertThat(restored.properties()).hasSize(2);
        assertThat(restored.properties().get(0).identifier()).isEqualTo("rainfall");
        assertThat(restored.properties().get(0).value()).isEqualTo(25.5);
        assertThat(restored.properties().get(0).unit()).isEqualTo("mm");
        assertThat(restored.properties().get(1).identifier()).isEqualTo("temperature");
        assertThat(restored.properties().get(1).value()).isEqualTo(18.2);
    }

    @Test
    @DisplayName("should handle empty properties list")
    void emptyProperties() {
        ParsedMessage msg = new ParsedMessage("D", "S", "sys", 0L, 0L, "h", List.of());
        String json = JSON.toJSONString(msg);
        ParsedMessage restored = JSON.parseObject(json, ParsedMessage.class);
        assertThat(restored.properties()).isEmpty();
    }

    @Test
    @DisplayName("should handle null property value")
    void nullPropertyValue() {
        ParsedMessage original = new ParsedMessage("D", "S", "sys", 0L, 0L, "h",
            List.of(new PropertyValue("id1", "name", "unit", null, 9)));
        String json = JSON.toJSONString(original);
        ParsedMessage restored = JSON.parseObject(json, ParsedMessage.class);
        assertThat(restored.properties().get(0).value()).isNull();
        assertThat(restored.properties().get(0).quality()).isEqualTo(9);
    }
}
