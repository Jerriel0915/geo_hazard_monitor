package com.zwei.framework.security.handle;

import java.io.IOException;
import java.io.Serializable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.zwei.common.constant.HttpStatus;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.ip.AddressUtils;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.common.utils.ServletUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.LogAttributes;
import com.zwei.log.domain.enums.AuthEventType;
import com.zwei.log.domain.enums.LogExecutionStatus;
import com.zwei.log.domain.model.LogAuthRecord;

/**
 * 认证失败处理类 返回未授权
 * 
 * @author zwei
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    private static final long serialVersionUID = -8970718410437077606L;

    private final LogCenterService logCenterService;

    public AuthenticationEntryPointImpl(LogCenterService logCenterService) {
        this.logCenterService = logCenterService;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        int code = HttpStatus.UNAUTHORIZED;
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
        LogAuthRecord record = new LogAuthRecord();
        record.setAuthEventType(AuthEventType.UNAUTHORIZED.name());
        record.setAuthChannel("TOKEN");
        record.setRequestUri(request.getRequestURI());
        record.setRequestMethod(request.getMethod());
        record.setClientIp(IpUtils.getIpAddr(request));
        record.setClientLocation(AddressUtils.getRealAddressByIP(record.getClientIp()));
        record.setUserAgent(StringUtils.substring(request.getHeader("User-Agent"), 0, 512));
        record.setHttpStatus(code);
        record.setResultStatus(LogExecutionStatus.FAIL.name());
        record.setFailureCode("UNAUTHORIZED");
        record.setFailureMessage(msg);
        Object traceId = request.getAttribute(LogAttributes.TRACE_ID);
        Object requestId = request.getAttribute(LogAttributes.REQUEST_ID);
        if (traceId != null) {
            record.setTraceId(traceId.toString());
        }
        if (requestId != null) {
            record.setRequestId(requestId.toString());
        }
        logCenterService.publishAuth(record);
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
