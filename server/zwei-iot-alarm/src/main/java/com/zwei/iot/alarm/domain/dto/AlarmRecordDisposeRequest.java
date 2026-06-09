package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 告警记录 — 处置请求
 *
 * @author zwei
 */
public class AlarmRecordDisposeRequest {

    /**
     * 新状态: 2=处理中 3=已销警 4=误报
     */
    @NotNull(message = "处置状态不能为空")
    private Integer status;

    /**
     * 处置备注
     */
    private String note;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
