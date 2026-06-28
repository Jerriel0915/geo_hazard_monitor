package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 综合告警策略 — 创建请求
 *
 * @author zwei
 */
public class StrategyCreateRequest {

    @NotBlank(message = "策略名称不能为空")
    private String name;

    private String description;

    /**
     * 监测类型ID（NULL=仅按隐患点绑定生效；非NULL=兜底策略）
     */
    private Long monitorTypeId;

    @NotBlank(message = "触发方式不能为空")
    @Pattern(regexp = "CRON|EVENT", message = "触发方式只能为CRON或EVENT")
    private String triggerMode;

    private String cronExpression;
    private String scriptType;

    @NotBlank(message = "脚本内容不能为空")
    private String scriptContent;

    @NotNull(message = "默认告警等级不能为空")
    @Min(value = 1, message = "告警等级范围为1-4")
    @Max(value = 4, message = "告警等级范围为1-4")
    private Integer defaultAlarmLevel;

    private Integer silenceMinutes;
    private Integer escalationEnabled;
    private Integer isEnabled;

    /**
     * hazard point scope values:
     * "*" = 全部隐患点; "group:{id}" = 按分组; "{数字}" = 指定隐患点ID
     */
    private String[] hazardPointIds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getMonitorTypeId() {
        return monitorTypeId;
    }

    public void setMonitorTypeId(Long monitorTypeId) {
        this.monitorTypeId = monitorTypeId;
    }

    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getScriptType() {
        return scriptType;
    }

    public void setScriptType(String scriptType) {
        this.scriptType = scriptType;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public Integer getDefaultAlarmLevel() {
        return defaultAlarmLevel;
    }

    public void setDefaultAlarmLevel(Integer defaultAlarmLevel) {
        this.defaultAlarmLevel = defaultAlarmLevel;
    }

    public Integer getSilenceMinutes() {
        return silenceMinutes;
    }

    public void setSilenceMinutes(Integer silenceMinutes) {
        this.silenceMinutes = silenceMinutes;
    }

    public Integer getEscalationEnabled() {
        return escalationEnabled;
    }

    public void setEscalationEnabled(Integer escalationEnabled) {
        this.escalationEnabled = escalationEnabled;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String[] getHazardPointIds() {
        return hazardPointIds;
    }

    public void setHazardPointIds(String[] hazardPointIds) {
        this.hazardPointIds = hazardPointIds;
    }
}
