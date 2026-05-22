package com.zwei.web.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.framework.event.OperLogEventListener;

/**
 * 日志SSE实时推送
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/common")
public class LogSseController {

    private final OperLogEventListener operLogEventListener;

    @Autowired
    public LogSseController(OperLogEventListener operLogEventListener) {
        this.operLogEventListener = operLogEventListener;
    }

    /**
     * SSE日志流端点
     * 前端通过 EventSource 连接此端点实时接收日志
     */
    @GetMapping("/logs/stream")
    public SseEmitter logStream() {
        return operLogEventListener.register();
    }

    /**
     * 获取当前活跃连接数（用于监控）
     */
    @GetMapping("/logs/connection-count")
    public int getConnectionCount() {
        return operLogEventListener.getActiveConnectionCount();
    }
}