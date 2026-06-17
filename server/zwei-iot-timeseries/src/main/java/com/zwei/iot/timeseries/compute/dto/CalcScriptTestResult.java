package com.zwei.iot.timeseries.compute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CalcScriptTestResult {
    private boolean success;
    private Map<String, Object> result;
    private String error;
    private long executionTime;

    public static CalcScriptTestResult ok(Map<String, Object> result, long elapsed) {
        CalcScriptTestResult r = new CalcScriptTestResult();
        r.success = true;
        r.result = result;
        r.executionTime = elapsed;
        return r;
    }

    public static CalcScriptTestResult fail(String error) {
        CalcScriptTestResult r = new CalcScriptTestResult();
        r.success = false;
        r.error = error;
        return r;
    }

    public boolean isSuccess() { return success; }
    public Map<String, Object> getResult() { return result; }
    public String getError() { return error; }
    public long getExecutionTime() { return executionTime; }
}
