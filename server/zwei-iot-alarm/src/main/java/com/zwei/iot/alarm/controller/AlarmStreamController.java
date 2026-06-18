package com.zwei.iot.alarm.controller;

import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.service.notify.AlarmStreamPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 告警 SSE 实时推送端点。
 *
 * <p>订阅时绑定当前登录 userId，使 SYSTEM 渠道的通知能够通过
 * {@code publishToUser(userId, ...)} 单点定向推送，避免跨用户广播泄漏。</p>
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/stream")
public class AlarmStreamController {

    private final AlarmStreamPublisher streamPublisher;

    public AlarmStreamController(AlarmStreamPublisher streamPublisher) {
        this.streamPublisher = streamPublisher;
    }

    @GetMapping
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public SseEmitter subscribe() {
        Long userId = SecurityUtils.getUserId();
        return streamPublisher.subscribe(userId);
    }
}
