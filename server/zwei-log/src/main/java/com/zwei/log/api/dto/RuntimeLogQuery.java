package com.zwei.log.api.dto;

import java.util.Date;

/**
 * 运行日志查询条件
 *
 * @author zwei
 */
public class RuntimeLogQuery {

    private String level;
    private String loggerName;
    private String keyword;
    private Date startTime;
    private Date endTime;

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
