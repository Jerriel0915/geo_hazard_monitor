package com.zwei.iot.alarm.domain;

/**
 * 告警判据 — 单个条件模型（A+B+C: 主语+运算符+阈值）。
 *
 * <pre>
 * 直接监测内容: {"subject":"water_level","subjectType":"CONTENT","operator":"GT","threshold":8.0,"unit":"m"}
 * 函数主语:     {"subject":"hourly_avg","subjectType":"FUNCTION","function":"AVG","functionParams":{"period":"1h","sourceSubject":"rainfall"},"operator":"GT","threshold":50.0}
 * </pre>
 *
 * @author zwei
 */
public class LevelCondition {

    /**
     * 主语标识 — monitor_content.code 或函数名
     */
    private String subject;

    /**
     * 主语类型: CONTENT / FUNCTION
     */
    private String subjectType = "CONTENT";

    /**
     * 函数名（subjectType=FUNCTION 时）: AVG / MAX / MIN / SUM
     */
    private String function;

    /**
     * 函数参数 JSON: {"period":"1h","sourceSubject":"rainfall"}
     */
    private java.util.Map<String, Object> functionParams;

    /**
     * 运算符: GT / GTE / LT / LTE / EQ / NEQ / BETWEEN
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

    // ── Getters / Setters ──
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public java.util.Map<String, Object> getFunctionParams() {
        return functionParams;
    }

    public void setFunctionParams(java.util.Map<String, Object> functionParams) {
        this.functionParams = functionParams;
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
