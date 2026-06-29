package com.zwei.terra.agent.duty;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 值守模式 WebSocket 消息处理器。
 *
 * <p>处理来自 dashboard 前端的消息：
 * <ul>
 *   <li>{@code chat_message} — 用户发送的聊天消息，转交 TerraDutyService 处理</li>
 *   <li>{@code query/response} — 前端对后端查询的响应</li>
 * </ul>
 */
@Component
@Slf4j
public class TerraDutyWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TerraDutySessionManager sessionManager;

    @Autowired
    private TerraDutyService dutyService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionManager.register(session);

        // 发送握手响应
        sessionManager.sendTo(session, DutyProtocol.handshake());

        // 从 session attributes 获取用户信息
        Long userId = (Long) session.getAttributes().get(TerraDutyHandshakeInterceptor.ATTR_USER_ID);
        String username = (String) session.getAttributes().get(TerraDutyHandshakeInterceptor.ATTR_USERNAME);

        log.info("值守模式 Dashboard 已连接: sessionId={}, userId={}, username={}",
                session.getId(), userId, username);

        // 通知 DutyService 有新的值守连接
        dutyService.onConnect(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("收到 Dashboard 消息: sessionId={}, payload={}", session.getId(), payload);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");
            String namespace = (String) msg.getOrDefault("namespace", "");
            Map<String, Object> msgPayload = (Map<String, Object>) msg.getOrDefault("payload", Map.of());

            if ("command".equals(type) && "core".equals(namespace)) {
                // 用户聊天消息
                String action = (String) msgPayload.get("action");
                if ("chat_message".equals(action)) {
                    Map<String, Object> params = (Map<String, Object>) msgPayload.getOrDefault("params", Map.of());
                    String text = params.get("message") != null ? params.get("message").toString() : "";
                    handleChatMessage(session, text);
                }
            } else if ("response".equals(type)) {
                // 前端查询响应（如 get_state 响应）
                log.debug("收到前端响应: sessionId={}, payload={}", session.getId(), msgPayload);
            }

        } catch (Exception e) {
            log.error("处理 Dashboard 消息异常: sessionId={}", session.getId(), e);
            sessionManager.sendTo(session, DutyProtocol.error("消息处理异常: " + e.getMessage()));
        }
    }

    /**
     * 处理用户聊天消息 — 调用 TerraDutyService 异步处理。
     */
    private void handleChatMessage(WebSocketSession session, String message) {
        Long userId = (Long) session.getAttributes().get(TerraDutyHandshakeInterceptor.ATTR_USER_ID);

        // 异步处理，不阻塞 WebSocket 线程
        dutyService.handleChat(session, message, userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("值守模式 WebSocket 传输错误: sessionId={}", session.getId(), exception);
        sessionManager.unregister(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.unregister(session);
        dutyService.onDisconnect(session);
        log.info("值守模式 Dashboard 已断开: sessionId={}, status={}", session.getId(), status);
    }
}
