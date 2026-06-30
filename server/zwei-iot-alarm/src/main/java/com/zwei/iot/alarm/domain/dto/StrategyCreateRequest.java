package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotBlank;
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
    @Pattern(regexp = "CRON|REALTIME", message = "触发方式只能为CRON或REALTIME")
    private String triggerMode;

    private String cronExpression;
    private String scriptType;

    @NotBlank(message = "脚本内容不能为空")
    private String scriptContent;

    private Integer silenceMinutes;
    private Integer sustainSeconds;
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

    public Integer getSilenceMinutes() {
        return silenceMinutes;
    }

    public void setSilenceMinutes(Integer silenceMinutes) {
        this.silenceMinutes = silenceMinutes;
    }

    public Integer getSustainSeconds() {
        return sustainSeconds;
    }

    public void setSustainSeconds(Integer sustainSeconds) {
        this.sustainSeconds = sustainSeconds;
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
