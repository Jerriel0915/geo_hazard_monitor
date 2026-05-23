package com.zwei.log.api.dto;

import java.util.Date;

/**
 * 认证日志查询条件
 *
 * @author zwei
 */
public class AuthLogQuery {

    private String username;
    private String authEventType;
    private String resultStatus;
    private Date startTime;
    private Date endTime;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthEventType() { return authEventType; }
    public void setAuthEventType(String authEventType) { this.authEventType = authEventType; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
