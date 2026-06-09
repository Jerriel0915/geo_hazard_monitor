package com.zwei.iot.alarm.domain;

import java.io.Serializable;

/**
 * 判据条件 VO — 对应 alarm_criteria.conditions_json 中单个条件元素。
 *
 * <pre>
 * 示例 JSON:
 * {"indicator":"value","operator":"GT","threshold":10.5,"unit":"mm"}
 * </pre>
 *
 * @author zwei
 */
public class CriteriaCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 指标标识（如 "value", "temperature"），对应 attrCode
     */
    private String indicator;
    /**
     * 操作符: GT, GTE, LT, LTE, EQ, NEQ, BETWEEN, RATE_CHANGE, ACCUMULATED
     */
    private String operator;
    /**
     * 阈值
     */
    private Double threshold;
    /**
     * 阈值上限（BETWEEN 时使用）
     */
    private Double thresholdMax;
    /**
     * 单位
     */
    private String unit;

    public CriteriaCondition() {
    }

    public CriteriaCondition(String indicator, String operator, Double threshold, String unit) {
        this.indicator = indicator;
        this.operator = operator;
        this.threshold = threshold;
        this.unit = unit;
    }

    // Getters & Setters
    public String getIndicator() {
        return indicator;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Double getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(Double thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
