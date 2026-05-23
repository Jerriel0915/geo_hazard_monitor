package com.zwei.framework.security.handle;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import com.alibaba.fastjson2.JSON;
import com.zwei.common.constant.Constants;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.utils.ip.AddressUtils;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.common.utils.MessageUtils;
import com.zwei.common.utils.ServletUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.framework.web.service.TokenService;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.LogAttributes;
import com.zwei.log.domain.enums.AuthEventType;
import com.zwei.log.domain.enums.LogExecutionStatus;
import com.zwei.log.domain.model.LogAuthRecord;

/**
 * 自定义退出处理类 返回成功
 * 
 * @author zwei
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private LogCenterService logCenterService;

    /**
     * 退出处理
     * 
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            LogAuthRecord record = new LogAuthRecord();
            record.setUserId(loginUser.getUserId());
            record.setUsername(userName);
            record.setAuthEventType(AuthEventType.LOGOUT.name());
            record.setAuthChannel("TOKEN");
            record.setRequestUri(request.getRequestURI());
            record.setRequestMethod(request.getMethod());
            record.setClientIp(IpUtils.getIpAddr(request));
            record.setClientLocation(AddressUtils.getRealAddressByIP(record.getClientIp()));
            record.setUserAgent(StringUtils.substring(request.getHeader("User-Agent"), 0, 512));
            record.setResultStatus(LogExecutionStatus.SUCCESS.name());
            record.setFailureMessage(MessageUtils.message("user.logout.success"));
            Object traceId = request.getAttribute(LogAttributes.TRACE_ID);
            Object requestId = request.getAttribute(LogAttributes.REQUEST_ID);
            if (traceId != null) {
                record.setTraceId(traceId.toString());
            }
            if (requestId != null) {
                record.setRequestId(requestId.toString());
            }
            logCenterService.publishAuth(record);
        }
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
