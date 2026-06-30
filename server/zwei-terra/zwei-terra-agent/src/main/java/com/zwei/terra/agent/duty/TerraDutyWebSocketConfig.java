package com.zwei.terra.agent.duty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 值守模式 WebSocket 配置 — 注册 /ws/terramens/duty 端点。
 */
@Configuration
@EnableWebSocket
@Slf4j
public class TerraDutyWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TerraDutyWebSocketHandler handler;

    @Autowired
    private TerraDutyHandshakeInterceptor interceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/terramens/duty")
                .addInterceptors(interceptor)
                .setAllowedOrigins("*");

        log.info("值守模式 WebSocket 端点已注册: /ws/terramens/duty");
    }
}
