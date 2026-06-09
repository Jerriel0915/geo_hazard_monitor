package com.zwei.iot.alarm.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 告警判据 — 创建请求
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
     * 判据条件 JSON
     */
    private String conditionsJson;
    /**
     * 逻辑运算符: AND/OR
     */
    private String logicOperator;

    private String blueExpression;
    private String blueDescription;
    private String yellowExpression;
    private String yellowDescription;
    private String orangeExpression;
    private String orangeDescription;
    private String redExpression;
    private String redDescription;

    private Integer persistCount;
    private Integer silencePeriod;
    private Integer isEnabled;

    // Getters and Setters
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

    public String getConditionsJson() {
        return conditionsJson;
    }

    public void setConditionsJson(String conditionsJson) {
        this.conditionsJson = conditionsJson;
    }

    public String getLogicOperator() {
        return logicOperator;
    }

    public void setLogicOperator(String logicOperator) {
        this.logicOperator = logicOperator;
    }

    public String getBlueExpression() {
        return blueExpression;
    }

    public void setBlueExpression(String blueExpression) {
        this.blueExpression = blueExpression;
    }

    public String getBlueDescription() {
        return blueDescription;
    }

    public void setBlueDescription(String blueDescription) {
        this.blueDescription = blueDescription;
    }

    public String getYellowExpression() {
        return yellowExpression;
    }

    public void setYellowExpression(String yellowExpression) {
        this.yellowExpression = yellowExpression;
    }

    public String getYellowDescription() {
        return yellowDescription;
    }

    public void setYellowDescription(String yellowDescription) {
        this.yellowDescription = yellowDescription;
    }

    public String getOrangeExpression() {
        return orangeExpression;
    }

    public void setOrangeExpression(String orangeExpression) {
        this.orangeExpression = orangeExpression;
    }

    public String getOrangeDescription() {
        return orangeDescription;
    }

    public void setOrangeDescription(String orangeDescription) {
        this.orangeDescription = orangeDescription;
    }

    public String getRedExpression() {
        return redExpression;
    }

    public void setRedExpression(String redExpression) {
        this.redExpression = redExpression;
    }

    public String getRedDescription() {
        return redDescription;
    }

    public void setRedDescription(String redDescription) {
        this.redDescription = redDescription;
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
        this.isEnabled = isEnabled;
    }
}
