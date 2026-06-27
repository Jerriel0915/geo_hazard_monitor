package com.zwei.iot.alarm.domain.dto;

public class StrategyTestRunResult {
    private Integer level;
    private String levelText;
    private long durationMs;
    private String error;

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getLevelText() { return levelText; }
    public void setLevelText(String levelText) { this.levelText = levelText; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
