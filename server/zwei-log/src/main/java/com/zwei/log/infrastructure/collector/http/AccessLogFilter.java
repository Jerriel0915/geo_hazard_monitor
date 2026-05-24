package com.zwei.log.infrastructure.collector.http;

import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.ip.AddressUtils;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.common.utils.uuid.UUID;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.LogAttributes;
import com.zwei.log.domain.enums.LogExecutionStatus;
import com.zwei.log.domain.model.LogOperationRecord;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

/**
 * 全接口访问日志采集过滤器
 *
 * @author zwei
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    private final LogCenterService logCenterService;

    public AccessLogFilter(LogCenterService logCenterService) {
        this.logCenterService = logCenterService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/")
                || path.startsWith("/api/v1/logs/")
                || path.startsWith("/api/v1/auth/")
                || "/register".equals(path)
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        long start = System.currentTimeMillis();
        request.setAttribute(LogAttributes.TRACE_ID, UUID.fastUUID().toString(true));
        request.setAttribute(LogAttributes.REQUEST_ID, UUID.fastUUID().toString(true));
        Exception error = null;
        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            error = ex;
            throw ex;
        } finally {
            if (Boolean.TRUE.equals(request.getAttribute(LogAttributes.ASPECT_HANDLED))) {
                return;
            }
            try {
                LogOperationRecord record = new LogOperationRecord();
                record.setOccurredAt(new Date());
                record.setTraceId(String.valueOf(request.getAttribute(LogAttributes.TRACE_ID)));
                record.setRequestId(String.valueOf(request.getAttribute(LogAttributes.REQUEST_ID)));
                record.setTitle("接口访问");
                record.setBusinessType("REQUEST");
                record.setApiPath(StringUtils.substring(request.getRequestURI(), 0, 255));
                record.setRequestMethod(request.getMethod());
                record.setControllerMethod("FILTER");
                record.setClientIp(IpUtils.getIpAddr(request));
                record.setClientLocation(AddressUtils.getRealAddressByIP(record.getClientIp()));
                record.setUserAgent(StringUtils.substring(request.getHeader("User-Agent"), 0, 512));
                record.setHttpStatus(response.getStatus());
                record.setCostTimeMs(System.currentTimeMillis() - start);
                record.setExecStatus(error == null && response.getStatus() < 500
                        ? LogExecutionStatus.SUCCESS.name()
                        : LogExecutionStatus.FAIL.name());
                if (error != null) {
                    record.setErrorMessage(StringUtils.substring(error.getMessage(), 0, 2000));
                }
                fillUser(record);
                logCenterService.publishOperation(record);
            } catch (Exception ex) {
                log.warn("[AccessLogFilter] 访问日志采集失败，已忽略。uri={}", request.getRequestURI(), ex);
            }
        }
    }

    private void fillUser(LogOperationRecord record) {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            return;
        }
        record.setUserId(loginUser.getUserId());
        record.setUsername(loginUser.getUsername());
        SysUser user = loginUser.getUser();
        if (user != null && user.getDept() != null) {
            record.setDeptName(user.getDept().getDeptName());
        }
    }
}
