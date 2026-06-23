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
     * 运算符: GT / GTE / LT / LTE / EQ / NEQ / BETWEEN / CONTAINS
     */
    private String operator;

    /**
     * 数据类型: NUMBER / DATETIME / STRING / BOOLEAN
     * <p>用于 CriteriaEvaluator 多态分派；为 null 时按 NUMBER 兼容老数据。
     */
    private String valueType;

    /**
     * 阈值 — 按 valueType 解释:
     * <ul>
     *   <li>NUMBER: Double</li>
     *   <li>DATETIME: String (ISO-8601 或 "now-5h" 相对)</li>
     *   <li>STRING: String</li>
     *   <li>BOOLEAN: Integer (1/0)</li>
     * </ul>
     */
    private Object threshold;

    /**
     * 阈值上限（BETWEEN 时使用） — NUMBER: Double；DATETIME: String
     */
    private Object thresholdMax;

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

    public Object getThreshold() {
        return threshold;
    }

    public void setThreshold(Object threshold) {
        this.threshold = threshold;
    }

    public Object getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(Object thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
