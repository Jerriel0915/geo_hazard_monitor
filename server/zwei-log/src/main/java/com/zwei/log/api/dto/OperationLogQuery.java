package com.zwei.log.api.dto;

import java.util.Date;

/**
 * 接口调用日志查询条件
 *
 * @author zwei
 */
public class OperationLogQuery {

    private String username;
    private String keyword;
    private String execStatus;
    private Date startTime;
    private Date endTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getExecStatus() { return execStatus; }
    public void setExecStatus(String execStatus) { this.execStatus = execStatus; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
