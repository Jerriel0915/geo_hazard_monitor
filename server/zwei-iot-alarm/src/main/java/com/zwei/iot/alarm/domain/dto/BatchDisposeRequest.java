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

    /**
     * 处置描述 (FEEDBACK 时附带的详细描述)
     */
    private String description;
    /**
     * 附件文件名，多个逗号分隔 (/common/upload 返回的 fileName)
     */
    private String attachments;
    /**
     * 备注/反馈内容 (等价于 note，新前端优先使用 remarks)
     */
    private String remarks;

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAttachments() { return attachments; }
    public void setAttachments(String attachments) { this.attachments = attachments; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
