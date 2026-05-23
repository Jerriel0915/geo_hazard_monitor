package com.zwei.log.domain.model;

import com.zwei.log.domain.enums.LogType;

/**
 * 接口调用日志
 *
 * @author zwei
 */
public class LogOperationRecord extends AbstractLogRecord {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String deptName;
    private String title;
    private String businessType;
    private String apiPath;
    private String requestMethod;
    private String controllerMethod;
    private String clientIp;
    private String clientLocation;
    private String userAgent;
    private String requestParams;
    private String responseBody;
    private Integer httpStatus;
    private String execStatus;
    private String errorMessage;
    private Long costTimeMs;

    @Override
    public LogType getLogType() {
        return LogType.OPERATION;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }
    public String getControllerMethod() { return controllerMethod; }
    public void setControllerMethod(String controllerMethod) { this.controllerMethod = controllerMethod; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getClientLocation() { return clientLocation; }
    public void setClientLocation(String clientLocation) { this.clientLocation = clientLocation; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRequestParams() { return requestParams; }
    public void setRequestParams(String requestParams) { this.requestParams = requestParams; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    public String getExecStatus() { return execStatus; }
    public void setExecStatus(String execStatus) { this.execStatus = execStatus; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getCostTimeMs() { return costTimeMs; }
    public void setCostTimeMs(Long costTimeMs) { this.costTimeMs = costTimeMs; }
}
