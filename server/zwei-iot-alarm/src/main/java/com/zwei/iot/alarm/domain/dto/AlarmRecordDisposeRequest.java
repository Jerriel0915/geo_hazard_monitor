package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    @Min(value = 2, message = "处置状态只能为2/3/4")
    @Max(value = 4, message = "处置状态只能为2/3/4")
    private Integer status;

    /**
     * 处置备注
     */
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
