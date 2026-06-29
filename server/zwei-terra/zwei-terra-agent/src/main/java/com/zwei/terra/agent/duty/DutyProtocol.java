package com.zwei.terra.agent.duty;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Terramens Dashboard WebSocket 协议消息构建器。
 *
 * <p>消息信封格式：
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "id": "msg-{timestamp}-{random}",
 *   "timestamp": 1690000000000,
 *   "type": "command|event|query|response|error",
 *   "namespace": "core|panel|terra|data",
 *   "payload": { ... }
 * }
 * }</pre>
 */
@Slf4j
public class DutyProtocol {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 消息类型 */
    public static final String TYPE_COMMAND = "command";
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_QUERY = "query";
    public static final String TYPE_RESPONSE = "response";
    public static final String TYPE_ERROR = "error";

    /** 命名空间 */
    public static final String NS_CORE = "core";
    public static final String NS_PANEL = "panel";
    public static final String NS_TERRA = "terra";
    public static final String NS_DATA = "data";

    // ==================== 握手响应 ====================

    /**
     * 构建握手响应消息 — 连接建立后发送，声明服务端支持的能力。
     */
    public static String handshake() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("supportedNamespaces", new String[]{NS_CORE, NS_PANEL, NS_TERRA, NS_DATA});
        capabilities.put("supportedActions", new String[]{
                "show", "hide", "update", "setData",
                "lifecycle:create", "lifecycle:destroy",
                "map:drawCircle", "map:setView", "map:fitBounds",
                "map:clearShapes", "video:play", "video:pause",
                "table:highlightRow", "chart:updateDataset"
        });
        return buildMessage(TYPE_RESPONSE, NS_CORE, capabilities);
    }

    // ==================== 事件消息 (后端→前端) ====================

    /**
     * 构建时间线消息事件 — 推送一条 timeline item 到前端。
     *
     * @param messageType 消息类型：thinking / observation / warning / action
     * @param message     消息内容（Markdown）
     * @param sender      发送者：user / terra
     * @param isStreaming  是否为流式消息
     * @param runId       流式消息分组 ID（同 runId 的消息会合并）
     */
    public static String timelineItem(String messageType, String message,
                                       String sender, boolean isStreaming, String runId) {
        Map<String, Object> timelineItem = new LinkedHashMap<>();
        timelineItem.put("type", messageType);
        timelineItem.put("message", message);
        timelineItem.put("timestamp", System.currentTimeMillis());
        if (sender != null) {
            timelineItem.put("sender", sender);
        }
        if (isStreaming) {
            timelineItem.put("isStreaming", true);
        }
        if (runId != null) {
            timelineItem.put("runId", runId);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timelineItem", timelineItem);
        return buildMessage(TYPE_EVENT, NS_TERRA, payload);
    }

    /**
     * 构建非流式时间线消息。
     */
    public static String timelineItem(String messageType, String message, String sender) {
        return timelineItem(messageType, message, sender, false, null);
    }

    /**
     * 构建流式时间线消息（增量）。
     */
    public static String streamingTimelineItem(String message, String runId) {
        return timelineItem("thinking", message, "terra", true, runId);
    }

    /**
     * 构建流式完成时间线消息。
     */
    public static String streamingCompleteTimelineItem(String message, String runId) {
        return timelineItem("thinking", message, "terra", false, runId);
    }

    /**
     * 构建 Terra 状态更新事件。
     *
     * @param state    头像状态：normal / info / caution / warning / critical
     * @param message  状态描述
     */
    public static String terraState(String state, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("terraState", state);
        if (message != null) {
            payload.put("message", message);
        }
        return buildMessage(TYPE_EVENT, NS_TERRA, payload);
    }

    /**
     * 构建心跳触发事件 — 让前端播放头像心跳动画。
     */
    public static String heartbeatTrigger() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("heartbeatTrigger", true);
        return buildMessage(TYPE_EVENT, NS_TERRA, payload);
    }

    /**
     * 构建告警事件。
     *
     * @param level       告警级别：attention / warning / critical
     * @param title       告警标题
     * @param description 告警描述
     */
    public static String alert(String level, String title, String description) {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("level", level);
        alert.put("title", title);
        alert.put("description", description);
        alert.put("timestamp", System.currentTimeMillis());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("alert", alert);
        return buildMessage(TYPE_EVENT, NS_TERRA, payload);
    }

    // ==================== 面板命令 (后端→前端) ====================

    /**
     * 构建创建面板命令。
     *
     * @param panelId  面板唯一 ID
     * @param type     面板类型：map / video / chart / table / image / iframe
     * @param title    面板标题
     * @param data     面板初始数据
     * @param position 位置 {x, y, w, h}（12 列 x 12 行网格）
     */
    public static String createPanel(String panelId, String type, String title,
                                      Object data, Map<String, Object> position) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("panelId", panelId);
        params.put("type", type);
        params.put("title", title);
        params.put("data", data);
        params.put("position", position != null ? position : defaultPosition());
        params.put("zIndex", 1);
        params.put("visible", true);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "lifecycle:create");
        payload.put("params", params);
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建销毁面板命令。
     */
    public static String destroyPanel(String panelId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", panelId);
        payload.put("action", "lifecycle:destroy");
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建设置面板数据命令。
     */
    public static String setPanelData(String panelId, Object data) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("data", data);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", panelId);
        payload.put("action", "setData");
        payload.put("params", params);
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建更新面板命令。
     */
    public static String updatePanel(String panelId, String field, Object value) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(field, value);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", panelId);
        payload.put("action", "update");
        payload.put("params", params);
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建清除所有面板命令。
     */
    public static String clearAllPanels() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("action", "clearAll");
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建地图导航命令。
     *
     * @param panelId 地图面板 ID
     * @param lat     纬度
     * @param lng     经度
     * @param zoom    缩放级别
     */
    public static String mapSetView(String panelId, double lat, double lng, int zoom) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("center", lat + "," + lng);
        params.put("zoom", zoom);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", panelId);
        payload.put("action", "map:setView");
        payload.put("params", params);
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    /**
     * 构建通用面板命令。
     */
    public static String panelCommand(String panelId, String action, Object params) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("target", panelId);
        payload.put("action", action);
        payload.put("params", params);
        return buildMessage(TYPE_COMMAND, NS_PANEL, payload);
    }

    // ==================== 错误消息 ====================

    public static String error(String errorMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", errorMessage);
        return buildMessage(TYPE_ERROR, NS_CORE, payload);
    }

    // ==================== 查询消息 ====================

    /**
     * 构建状态查询消息 — 向前端查询当前面板状态。
     */
    public static String queryState() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", "get_state");
        return buildMessage(TYPE_QUERY, NS_CORE, payload);
    }

    // ==================== 内部方法 ====================

    /**
     * 构建完整的 WebSocket 协议消息。
     */
    private static String buildMessage(String type, String namespace, Object payload) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("version", "1.0");
        message.put("id", "msg-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 9));
        message.put("timestamp", System.currentTimeMillis());
        message.put("type", type);
        message.put("namespace", namespace);
        message.put("payload", payload);
        return toJson(message);
    }

    private static Map<String, Object> defaultPosition() {
        Map<String, Object> pos = new LinkedHashMap<>();
        pos.put("x", 1);
        pos.put("y", 1);
        pos.put("w", 6);
        pos.put("h", 4);
        return pos;
    }

    private static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("序列化 WebSocket 消息失败", e);
            return "{\"type\":\"error\",\"payload\":{\"message\":\"消息序列化失败\"}}";
        }
    }
}
