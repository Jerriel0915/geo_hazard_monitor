package com.zwei.terra.agent.duty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 值守模式 WebSocket 会话管理器。
 *
 * <p>管理所有活跃的 dashboard 连接，提供广播和定向发送能力。</p>
 */
@Component
@Slf4j
public class TerraDutySessionManager {

    /** 活跃 WebSocket 会话集合 */
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    /**
     * 注册新会话。
     */
    public void register(WebSocketSession session) {
        sessions.add(session);
        log.info("值守模式 WebSocket 会话已连接: id={}, 总连接数={}", session.getId(), sessions.size());
    }

    /**
     * 注销会话。
     */
    public void unregister(WebSocketSession session) {
        sessions.remove(session);
        log.info("值守模式 WebSocket 会话已断开: id={}, 剩余连接数={}", session.getId(), sessions.size());
    }

    /**
     * 向指定会话发送消息。
     */
    public void sendTo(WebSocketSession session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.warn("发送 WebSocket 消息失败（会话可能已关闭）: sessionId={}, error={}",
                    session.getId(), e.getMessage());
        }
    }

    /**
     * 广播消息到所有连接。
     */
    public void broadcast(String message) {
        for (WebSocketSession session : sessions) {
            sendTo(session, message);
        }
    }

    /**
     * 广播消息到所有连接（排除指定会话）。
     */
    public void broadcastExcept(String message, WebSocketSession excludeSession) {
        for (WebSocketSession session : sessions) {
            if (!session.getId().equals(excludeSession != null ? excludeSession.getId() : "")) {
                sendTo(session, message);
            }
        }
    }

    /**
     * 获取第一个活跃会话（值守模式通常只有单一操作员）。
     */
    public WebSocketSession getFirstSession() {
        return sessions.stream().findFirst().orElse(null);
    }

    /**
     * 是否有活跃连接。
     */
    public boolean hasActiveSession() {
        return !sessions.isEmpty();
    }

    /**
     * 获取活跃连接数。
     */
    public int getSessionCount() {
        return sessions.size();
    }
}
