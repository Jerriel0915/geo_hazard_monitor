package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 告警判据 — 创建/更新请求 V3.0。
 *
 * @author zwei
 */
public class CriteriaCreateRequest {

    @NotBlank(message = "判据名称不能为空")
    private String name;

    private Long monitorTypeId;
    private String monitorTypeName;
    private Long monitorContentId;
    private String monitorContentCode;
    private Long hazardPointId;

    /**
     * 四级告警条件配置 JSON: {"blue":{...},"yellow":{...},"orange":{...},"red":{...}}
     */
    private String levelConfig;

    private Integer persistCount;
    private Integer silencePeriod;
    private Integer isEnabled;

    // ── Getters / Setters ──
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMonitorTypeId() {
        return monitorTypeId;
    }

    public void setMonitorTypeId(Long monitorTypeId) {
        this.monitorTypeId = monitorTypeId;
    }

    public String getMonitorTypeName() {
        return monitorTypeName;
    }

    public void setMonitorTypeName(String monitorTypeName) {
        this.monitorTypeName = monitorTypeName;
    }

    public Long getMonitorContentId() {
        return monitorContentId;
    }

    public void setMonitorContentId(Long monitorContentId) {
        this.monitorContentId = monitorContentId;
    }

    public String getMonitorContentCode() {
        return monitorContentCode;
    }

    public void setMonitorContentCode(String monitorContentCode) {
        this.monitorContentCode = monitorContentCode;
    }

    public Long getHazardPointId() {
        return hazardPointId;
    }

    public void setHazardPointId(Long hazardPointId) {
        this.hazardPointId = hazardPointId;
    }

    public String getLevelConfig() {
        return levelConfig;
    }

    public void setLevelConfig(String levelConfig) {
        this.levelConfig = levelConfig;
    }

    public Integer getPersistCount() {
        return persistCount;
    }

    public void setPersistCount(Integer persistCount) {
        this.persistCount = persistCount;
    }

    public Integer getSilencePeriod() {
        return silencePeriod;
    }

    public void setSilencePeriod(Integer silencePeriod) {
        this.silencePeriod = silencePeriod;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Integer isEnabled) {
        this.isEnabled = isEnabled; }
}
