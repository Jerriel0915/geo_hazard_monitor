package com.zwei.iot.alarm.controller;

import com.zwei.iot.alarm.service.notify.AlarmStreamPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 告警 SSE 实时推送端点。
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
    @PreAuthorize("hasAuthority('iot:alarm-record:list')")
    public SseEmitter subscribe() {
        return streamPublisher.subscribe();
    }
}
