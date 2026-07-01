package com.zwei.log.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.zwei.log.infrastructure.config.LogModuleProperties;
import com.zwei.log.infrastructure.tail.LogFileTailService;

/**
 * 控制台日志实时流 — 直接 tail 日志文件，以原始格式行推送
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogConsoleStreamController {

    private final LogFileTailService tailService;
    private final LogModuleProperties properties;

    public LogConsoleStreamController(LogFileTailService tailService, LogModuleProperties properties) {
        this.tailService = tailService;
        this.properties = properties;
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/console-stream")
    public SseEmitter consoleStream(
        @RequestParam(value = "window", required = false) Long windowMinutes) {
        SseEmitter emitter = new SseEmitter(properties.getSseTimeoutMs());
        long window = (windowMinutes != null && windowMinutes > 0)
            ? windowMinutes : properties.getConsoleReplayWindowMinutes();
        return tailService.subscribe(emitter, window);
    }
}
