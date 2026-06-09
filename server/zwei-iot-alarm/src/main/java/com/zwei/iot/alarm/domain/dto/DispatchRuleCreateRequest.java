package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 告警分发规则 — 创建请求
 *
 * @author zwei
 */
public class DispatchRuleCreateRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;
    private Long hazardPointId;
    private String alarmLevels;
    private String alarmTypes;
    private String recipientsJson;
    private String channels;
    private String timeWindow;
    private Integer isEnabled;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getHazardPointId() {
        return hazardPointId;
    }

    public void setHazardPointId(Long hazardPointId) {
        this.hazardPointId = hazardPointId;
    }

    public String getAlarmLevels() {
        return alarmLevels;
    }

    public void setAlarmLevels(String alarmLevels) {
        this.alarmLevels = alarmLevels;
    }

    public String getAlarmTypes() {
        return alarmTypes;
    }

    public void setAlarmTypes(String alarmTypes) {
        this.alarmTypes = alarmTypes;
    }

    public String getRecipientsJson() {
        return recipientsJson;
    }

    public void setRecipientsJson(String recipientsJson) {
        this.recipientsJson = recipientsJson;
    }

    public String getChannels() {
        return channels;
    }

    public void setChannels(String channels) {
        this.channels = channels;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
    }
}
