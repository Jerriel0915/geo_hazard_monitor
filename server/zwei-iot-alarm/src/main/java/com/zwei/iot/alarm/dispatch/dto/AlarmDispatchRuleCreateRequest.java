package com.zwei.iot.alarm.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 通知规则创建/编辑请求
 */
@Data
public class AlarmDispatchRuleCreateRequest {

    /** 编辑时必填 */
    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "事件类型不能为空（THRESHOLD/COMPREHENSIVE/OFFLINE）")
    private String eventType;

    /** THRESHOLD/COMPREHENSIVE 必填；OFFLINE 时为 null */
    private List<String> alarmLevels;

    @NotEmpty(message = "通知渠道不能为空")
    private List<String> channels;

    /** THRESHOLD/COMPREHENSIVE 必填；元素可为 "*" */
    private List<String> hazardPointIds;

    /** OFFLINE 必填；元素可为 "*" */
    private List<String> deviceIds;

    private RecipientSelection recipients;

    private Integer isEnabled = 1;

    private String remark;

    @Data
    public static class RecipientSelection {
        /** 可含 "*" */
        private List<String> roleIds;
        /** 可含 "*" */
        private List<String> deptIds;
        /** 可含 "*" */
        private List<String> userIds;
    }
}
