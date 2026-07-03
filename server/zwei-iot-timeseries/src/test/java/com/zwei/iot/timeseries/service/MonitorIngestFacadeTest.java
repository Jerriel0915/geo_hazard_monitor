package com.zwei.iot.timeseries.service;

import com.zwei.common.event.MqttMessageRejectEvent;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.MonitorMetadataService;
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MonitorIngestFacade — 异常报文事件发布")
class MonitorIngestFacadeTest {

    @Mock
    private MonitorTopicParser topicParser;
    @Mock
    private MonitorMetadataService metadataService;
    @Mock
    private GroovyScriptEngine scriptEngine;
    @Mock
    private MonitorIngestStreamService streamService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MonitorIngestFacade facade;

    private static final String CLIENT_ID = "client-abc";
    private static final String USERNAME = "NZMX40";
    private static final Long DEVICE_ID = 42L;
    private static final String TOPIC = "sys/v1/DEV001/SENS01/updata";
    private static final byte[] PAYLOAD = "{\"value\":1.23}".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        facade = new MonitorIngestFacade(topicParser, metadataService, scriptEngine, streamService, eventPublisher);
    }

    @Test
    @DisplayName("topic 格式无效时发布 reject 事件并携带 clientId/username")
    void shouldPublishRejectWithClientIdAndUsernameWhenTopicInvalid() {
        when(topicParser.parse(TOPIC)).thenReturn(null);

        facade.doIngest(TOPIC, PAYLOAD, DEVICE_ID, CLIENT_ID, USERNAME);

        var captor = ArgumentCaptor.forClass(MqttMessageRejectEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MqttMessageRejectEvent event = captor.getValue();

        assertThat(event.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(event.getTopic()).isEqualTo(TOPIC);
        assertThat(event.getRejectStage()).isEqualTo("FORMAT");
        assertThat(event.getRejectReason()).contains("Invalid monitor topic format");
    }

    @Test
    @DisplayName("无匹配策略时发布 reject 事件并携带 clientId/username")
    void shouldPublishRejectWithClientIdAndUsernameWhenNoStrategy() {
        MonitorTopic parsedTopic = new MonitorTopic("sys", "DEV001", "SENS01");
        when(topicParser.parse(TOPIC)).thenReturn(parsedTopic);
        when(metadataService.resolveStrategy(any(), anyLong())).thenReturn(null);

        facade.doIngest(TOPIC, PAYLOAD, DEVICE_ID, CLIENT_ID, USERNAME);

        var captor = ArgumentCaptor.forClass(MqttMessageRejectEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MqttMessageRejectEvent event = captor.getValue();

        assertThat(event.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getRejectStage()).isEqualTo("STRATEGY");
        assertThat(event.getRejectReason()).contains("No matching parse strategy");
    }

    @Test
    @DisplayName("Groovy 脚本解析失败时发布 reject 事件并携带 clientId/username")
    void shouldPublishRejectWithClientIdAndUsernameWhenParseFails() {
        MonitorTopic parsedTopic = new MonitorTopic("sys", "DEV001", "SENS01");
        DataParseStrategy strategy = new DataParseStrategy();
        strategy.setName("test-strategy");

        when(topicParser.parse(TOPIC)).thenReturn(parsedTopic);
        when(metadataService.resolveStrategy(any(), anyLong())).thenReturn(strategy);
        when(scriptEngine.execute(strategy, TOPIC, PAYLOAD)).thenReturn(null);

        facade.doIngest(TOPIC, PAYLOAD, DEVICE_ID, CLIENT_ID, USERNAME);

        var captor = ArgumentCaptor.forClass(MqttMessageRejectEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MqttMessageRejectEvent event = captor.getValue();

        assertThat(event.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getRejectStage()).isEqualTo("PARSE");
        assertThat(event.getRejectReason()).contains("test-strategy");
    }

    @Test
    @DisplayName("未知异常时发布 reject 事件 stage=UNKNOWN 并携带堆栈")
    void shouldPublishRejectWithUnknownStageOnGenericException() {
        MonitorTopic parsedTopic = new MonitorTopic("sys", "DEV001", "SENS01");
        DataParseStrategy strategy = new DataParseStrategy();
        strategy.setName("test-strategy");
        RuntimeException cause = new RuntimeException("Unexpected error");

        when(topicParser.parse(TOPIC)).thenReturn(parsedTopic);
        when(metadataService.resolveStrategy(any(), anyLong())).thenReturn(strategy);
        when(scriptEngine.execute(strategy, TOPIC, PAYLOAD)).thenThrow(cause);

        facade.doIngest(TOPIC, PAYLOAD, DEVICE_ID, CLIENT_ID, USERNAME);

        var captor = ArgumentCaptor.forClass(MqttMessageRejectEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MqttMessageRejectEvent event = captor.getValue();

        assertThat(event.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getRejectStage()).isEqualTo("UNKNOWN");
        assertThat(event.getRejectReason()).isEqualTo("Unexpected error");
        assertThat(event.getErrorStack()).isNotNull().contains("RuntimeException");
    }

    @Test
    @DisplayName("payload 为空时仍正常发布 reject 事件")
    void shouldHandleNullPayloadGracefully() {
        when(topicParser.parse(TOPIC)).thenReturn(null);

        facade.doIngest(TOPIC, null, DEVICE_ID, CLIENT_ID, USERNAME);

        var captor = ArgumentCaptor.forClass(MqttMessageRejectEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        MqttMessageRejectEvent event = captor.getValue();

        assertThat(event.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(event.getUsername()).isEqualTo(USERNAME);
        assertThat(event.getPayload()).isNull();
    }
}
