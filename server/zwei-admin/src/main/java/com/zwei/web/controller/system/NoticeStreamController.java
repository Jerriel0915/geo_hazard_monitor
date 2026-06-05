package com.zwei.web.controller.system;

import com.zwei.system.service.NoticeStreamPublisher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 通知公告 SSE 流控制器。
 * <p>
 * 提供实时推送通知公告的 SSE 端点，前端布局组件建立连接后
 * 即可在新通知发布时实时收到推送，无需轮询。
 */
@RestController
@RequestMapping("/api/v1/system/notice")
public class NoticeStreamController {

    private final NoticeStreamPublisher publisher;

    public NoticeStreamController(NoticeStreamPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 订阅通知公告 SSE 流。
     * <p>
     * 连接建立后发送 ready 事件，后续有新通知时推送 notice 事件。
     * 超时时间 5 分钟，超时后前端自动重连。
     */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return publisher.subscribe();
    }
}
