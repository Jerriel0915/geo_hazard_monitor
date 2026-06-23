package com.zwei.iot.device.domain;

import java.time.LocalDateTime;

/**
 * 监测统计数据持久化记录 (monitor_stats 表)。
 */
public class MonitorStat {

    private Long id;
    private String statKey;
    private Long statValue;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatKey() { return statKey; }
    public void setStatKey(String statKey) { this.statKey = statKey; }

    public Long getStatValue() { return statValue; }
    public void setStatValue(Long statValue) { this.statValue = statValue; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
