package com.zwei.log.domain.model;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zwei.common.core.domain.BaseEntity;
import com.zwei.log.domain.enums.LogType;

/**
 * 日志基础模型
 *
 * @author zwei
 */
public abstract class AbstractLogRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long eventId;

    private String traceId;

    private String requestId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date occurredAt;

    public abstract LogType getLogType();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Date occurredAt) {
        this.occurredAt = occurredAt;
    }
}
