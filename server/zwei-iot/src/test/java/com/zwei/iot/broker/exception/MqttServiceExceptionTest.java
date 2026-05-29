package com.zwei.iot.broker.exception;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.exception.base.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MQTT 异常体系测试")
class MqttServiceExceptionTest {

    @Test
    @DisplayName("异常体系应继承 BaseException 并保持分支层级关系")
    void hierarchy_shouldMatch() {
        MqttErrorContext context = MqttErrorContext.builder()
                .clientId("c1")
                .topic("sys/v1/1/S01/updata")
                .messageId("m1")
                .packetId(10)
                .qos(1)
                .protocolVersion("3.1.1")
                .brokerAddress("127.0.0.1:1883")
                .putAttribute("extra", "x")
                .build();

        MqttServiceException conn = new MqttConnectionException.Timeout(context, "连接超时");
        MqttServiceException comm = new MqttCommunicationException.PublishFailed(context, "发布失败");
        MqttServiceException proto = new MqttProtocolException.MalformedPacket(context, "报文格式错误");
        MqttServiceException biz = new MqttBusinessException.PermissionDenied(context, "权限不足");

        assertInstanceOf(BaseException.class, conn);
        assertInstanceOf(MqttConnectionException.class, conn);
        assertInstanceOf(MqttCommunicationException.class, comm);
        assertInstanceOf(MqttProtocolException.class, proto);
        assertInstanceOf(MqttBusinessException.class, biz);
    }

    @Test
    @DisplayName("异常属性应包含错误码、上下文与时间戳")
    void properties_shouldBeAssigned() {
        MqttErrorContext context = MqttErrorContext.builder()
                .clientId("c1")
                .topic("t1")
                .build();
        MqttServiceException exception = new MqttCommunicationException.PayloadParseFailed(context, "payload 解析失败");

        assertNotNull(exception.getTimestamp());
        assertEquals(MqttErrorCode.COMM_PAYLOAD_PARSE_FAILED.getCode(), exception.getErrorCode());
        assertEquals("c1", exception.getContext().getClientId());
        assertEquals("t1", exception.getContext().getTopic());
    }

    @Test
    @DisplayName("日志序列化应输出完整字段且为单行 JSON 格式")
    void serialization_shouldWork() {
        MqttErrorContext context = MqttErrorContext.builder()
                .clientId("c1")
                .topic("t1")
                .build();
        MqttServiceException exception = new MqttBusinessException.InvalidTopic(context, "主题非法", new IllegalArgumentException("bad topic"));

        Map<String, Object> logMap = exception.toLogMap();
        assertEquals(MqttErrorCode.BIZ_INVALID_TOPIC.getCode(), logMap.get("errorCode"));
        assertTrue(logMap.containsKey("timestamp"));
        assertTrue(logMap.containsKey("exception"));
        assertTrue(logMap.containsKey("message"));
        assertTrue(logMap.containsKey("context"));
        assertEquals(IllegalArgumentException.class.getName(), logMap.get("cause"));

        String logString = exception.toLogString();
        assertTrue(logString.startsWith("{"));
        assertTrue(logString.endsWith("}"));
        assertTrue(logString.contains("\"errorCode\""));
        assertTrue(logString.contains("\"context\""));
    }

    @Test
    @DisplayName("前端响应序列化应输出 AjaxResult.error 并携带 data.errorCode/context/timestamp")
    void ajaxResult_shouldWork() {
        MqttErrorContext context = MqttErrorContext.builder()
                .clientId("c1")
                .topic("t1")
                .build();
        MqttServiceException exception = new MqttConnectionException.AuthenticationFailed(context, "认证失败");

        AjaxResult result = exception.toAjaxResult();
        assertTrue(result.isError());
        Object data = result.get(AjaxResult.DATA_TAG);
        assertInstanceOf(Map.class, data);
        Map<?, ?> map = (Map<?, ?>) data;
        assertEquals(MqttErrorCode.CONN_AUTH_FAILED.getCode(), map.get("errorCode"));
        assertTrue(map.containsKey("timestamp"));
        assertTrue(map.containsKey("context"));
    }
}

