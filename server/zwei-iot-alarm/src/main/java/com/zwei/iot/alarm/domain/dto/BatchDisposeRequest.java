package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 告警记录 — 批量操作请求
 *
 * @author zwei
 */
public class BatchDisposeRequest {

    @NotEmpty(message = "告警ID列表不能为空")
    private List<Long> ids;

    @NotNull(message = "处置状态不能为空")
    private Integer status;

    private String note;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

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
