package com.zwei.iot.parser.support;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorTopicParserTest {

    @Mock
    private ITopicPatternService topicPatternService;

    private MonitorTopicParser parser;

    @BeforeEach
    void setUp() {
        parser = new MonitorTopicParser(topicPatternService);
    }

    @Test
    @DisplayName("parse extracts components from sys topic")
    void parse_sysTopic_returnsMonitorTopic() {
        when(topicPatternService.resolveTopic("sys/v1/DEV001/S01/updata"))
                .thenReturn(new TopicComponents("sys", "DEV001", "S01"));

        MonitorTopic result = parser.parse("sys/v1/DEV001/S01/updata");

        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("sys");
        assertThat(result.deviceCode()).isEqualTo("DEV001");
        assertThat(result.sensorCode()).isEqualTo("S01");
    }

    @Test
    @DisplayName("parse extracts components from gb topic")
    void parse_gbTopic_returnsMonitorTopic() {
        when(topicPatternService.resolveTopic("gb/v1/ABC/S_99/updata"))
                .thenReturn(new TopicComponents("gb", "ABC", "S_99"));

        MonitorTopic result = parser.parse("gb/v1/ABC/S_99/updata");

        assertThat(result).isNotNull();
        assertThat(result.sourceType()).isEqualTo("gb");
        assertThat(result.deviceCode()).isEqualTo("ABC");
        assertThat(result.sensorCode()).isEqualTo("S_99");
    }

    @Test
    @DisplayName("parse returns null when resolveTopic returns null")
    void parse_invalidTopic_returnsNull() {
        when(topicPatternService.resolveTopic(anyString())).thenReturn(null);

        assertThat(parser.parse("invalid")).isNull();
        assertThat(parser.parse((String) null)).isNull();
    }
}
