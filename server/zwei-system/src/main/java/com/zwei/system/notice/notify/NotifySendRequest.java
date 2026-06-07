package com.zwei.system.notice.notify;

/**
 * 通知发送请求 — 携带通道分发所需的全部上下文。
 */
public class NotifySendRequest {

    private final Long instanceId;
    private final Long userId;
    private final String channel;
    private final String title;
    private final String content;
    private final int priority;

    public NotifySendRequest(Long instanceId, Long userId, String channel,
                              String title, String content, int priority) {
        this.instanceId = instanceId;
        this.userId = userId;
        this.channel = channel;
        this.title = title;
        this.content = content;
        this.priority = priority;
    }

    public Long getInstanceId() { return instanceId; }
    public Long getUserId() { return userId; }
    public String getChannel() { return channel; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getPriority() { return priority; }
}
