package com.zwei.log.domain.model;

import com.zwei.log.domain.enums.LogType;

/**
 * 认证日志
 *
 * @author zwei
 */
public class LogAuthRecord extends AbstractLogRecord {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String authEventType;
    private String authChannel;
    private String requestUri;
    private String requestMethod;
    private String clientIp;
    private String clientLocation;
    private String userAgent;
    private String deviceType;
    private Integer httpStatus;
    private String resultStatus;
    private String failureCode;
    private String failureMessage;
    private String tokenId;

    @Override
    public LogType getLogType() {
        return LogType.AUTH;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAuthEventType() { return authEventType; }
    public void setAuthEventType(String authEventType) { this.authEventType = authEventType; }
    public String getAuthChannel() { return authChannel; }
    public void setAuthChannel(String authChannel) { this.authChannel = authChannel; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getClientLocation() { return clientLocation; }
    public void setClientLocation(String clientLocation) { this.clientLocation = clientLocation; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getFailureCode() { return failureCode; }
    public void setFailureCode(String failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public String getTokenId() { return tokenId; }
    public void setTokenId(String tokenId) { this.tokenId = tokenId; }
}
