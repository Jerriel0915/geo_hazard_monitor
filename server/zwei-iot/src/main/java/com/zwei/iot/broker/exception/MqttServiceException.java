package com.zwei.iot.broker.exception;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.exception.base.BaseException;
import com.zwei.common.utils.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQTT 服务异常基类（根节点）。
 * <p>
 * 继承自项目统一基础异常 {@link BaseException}，并补充 MQTT 场景的扩展属性：
 * <ul>
 *   <li>errorCode：稳定错误码（同时映射为 {@link BaseException#getCode()}）</li>
 *   <li>context：错误上下文（clientId/topic/messageId 等）</li>
 *   <li>timestamp：错误发生时间戳</li>
 * </ul>
 * <p>
 * 使用方式：
 * <pre>
 * throw new MqttConnectionException.Timeout(
 *     MqttErrorContext.builder().clientId(clientId).build(),
 *     "连接超时"
 * );
 * </pre>
 */
public abstract class MqttServiceException extends BaseException implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final long timestamp;
    private final MqttErrorContext context;

    protected MqttServiceException(MqttErrorCode errorCode, MqttErrorContext context, String message) {
        this(errorCode, context, message, null);
    }

    protected MqttServiceException(MqttErrorCode errorCode, MqttErrorContext context, String message, Throwable cause) {
        super("mqtt", null, null, message);
        this.errorCode = errorCode == null ? null : errorCode.getCode();
        this.timestamp = System.currentTimeMillis();
        this.context = context;
        if (cause != null) {
            initCause(cause);
        }
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public MqttErrorContext getContext() {
        return context;
    }

    /**
     * 序列化为标准化日志结构。
     *
     * @return 日志字段 Map，可直接交给日志采集系统结构化解析
     */
    public Map<String, Object> toLogMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("errorCode", errorCode);
        map.put("timestamp", timestamp);
        map.put("exception", getClass().getName());
        map.put("message", getMessage());
        if (context != null) {
            map.put("context", context.toMap());
        }
        Throwable cause = getCause();
        if (cause != null) {
            map.put("cause", cause.getClass().getName());
            map.put("causeMessage", cause.getMessage());
        }
        return map;
    }

    /**
     * 序列化为标准化日志文本。
     *
     * @return 单行文本，便于日志系统按行采集
     */
    public String toLogString() {
        Map<String, Object> map = toLogMap();
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append('"').append(':');
            sb.append(valueToJson(entry.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * 转换为前端可识别的错误响应结构。
     *
     * @return 标准 AjaxResult 错误响应
     */
    public AjaxResult toAjaxResult() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("errorCode", getErrorCode());
        data.put("timestamp", timestamp);
        if (context != null) {
            data.put("context", context.toMap());
        }
        data.put("exception", getClass().getName());
        return AjaxResult.error(getMessage(), data);
    }

    private String escape(String value) {
        return StringUtils.replace(StringUtils.replace(value, "\\", "\\\\"), "\"", "\\\"");
    }

    private String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder(128);
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                sb.append(valueToJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }
}
