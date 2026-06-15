package com.zwei.iot.parser.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonitorTopicParser")
class MonitorTopicParserTest {

    private final MonitorTopicParser parser = new MonitorTopicParser();

    @Test
    @DisplayName("should parse valid sys topic")
    void parseSysTopic() {
        MonitorTopic result = parser.parse("sys/v1/DEV001/S001/updata");
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("sys");
        assertThat(result.deviceCode()).isEqualTo("DEV001");
        assertThat(result.sensorCode()).isEqualTo("S001");
    }

    @Test
    @DisplayName("should parse valid gb topic")
    void parseGbTopic() {
        MonitorTopic result = parser.parse("gb/v1/MYDEVICE/MYSENSOR/updata");
        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("gb");
        assertThat(result.deviceCode()).isEqualTo("MYDEVICE");
        assertThat(result.sensorCode()).isEqualTo("MYSENSOR");
    }

    @Test
    @DisplayName("should reject invalid topic format")
    void rejectInvalidTopic() {
        assertThat(parser.parse("invalid/topic")).isNull();
        assertThat(parser.parse("sys/v1/DEV/updata")).isNull();  // missing sensor
        assertThat(parser.parse("")).isNull();
        assertThat(parser.parse(null)).isNull();
    }
}
