package com.zwei.log.infrastructure.sequence;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * 简单事件ID生成器
 *
 * @author zwei
 */
@Component
public class EventIdGenerator {

    /**
     * 预留 9xx 段给日志事件，确保运行期 eventId 始终大于历史迁移数据的 900/910 段。
     */
    private static final long BASE_EVENT_ID = 920_000_000_000_000_000L;

    private final AtomicLong sequence = new AtomicLong(BASE_EVENT_ID + System.currentTimeMillis() * 1000);

    public long nextId() {
        return sequence.incrementAndGet();
    }
}
