package com.zwei.common.event;

/**
 * 通知公告创建事件。
 * <p>
 * 在管理员发布通知公告后发布，由 NoticeStreamPublisher 消费并通过 SSE 实时推送给在线用户。
 */
public class NoticeCreatedEvent {

    private final Long noticeId;
    private final String title;
    private final String content;
    private final String type;
    private final String createTime;

    public NoticeCreatedEvent(Long noticeId, String title, String content, String type, String createTime) {
        this.noticeId = noticeId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.createTime = createTime;
    }

    public Long getNoticeId() { return noticeId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public String getCreateTime() { return createTime; }
}
