package com.zwei.iot.alarm.controller;

import com.zwei.common.annotation.Anonymous;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.utils.StringUtils;
import com.zwei.framework.web.service.TokenService;
import com.zwei.iot.alarm.service.notify.AlarmStreamPublisher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 告警 SSE 实时推送端点。
 *
 * <p>订阅时绑定当前登录 userId，使 SYSTEM 渠道的通知能够通过
 * {@code publishToUser(userId, ...)} 单点定向推送，避免跨用户广播泄漏。</p>
 *
 * <p>SSE 端点标记为 {@link Anonymous} 绕过 JWT 过滤器（浏览器 EventSource API
 * 不支持自定义请求头），通过查询参数传入 token 进行手动鉴权。</p>
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/stream")
public class AlarmStreamController {

    private final AlarmStreamPublisher streamPublisher;
    private final TokenService tokenService;

    public AlarmStreamController(AlarmStreamPublisher streamPublisher, TokenService tokenService) {
        this.streamPublisher = streamPublisher;
        this.tokenService = tokenService;
    }

    /**
     * 订阅告警 SSE 流
     * <p>
     * 需通过查询参数传入有效 token：{@code /api/v1/alarm/stream?token=xxx}。
     * token 无效时返回 401 纯文本错误，前端 EventSource 会触发 onerror 回调。
     */
    @Anonymous
    @GetMapping
    public SseEmitter subscribe(@RequestParam("token") String token, HttpServletRequest request) {
        if (StringUtils.isEmpty(token)) {
            throw new IllegalArgumentException("缺少 token 参数");
        }
        LoginUser loginUser = tokenService.getLoginUser(createRequestWrapper(request, token));
        if (loginUser == null) {
            throw new IllegalArgumentException("token 无效或已过期");
        }
        return streamPublisher.subscribe(loginUser.getUserId());
    }

    /**
     * 构造一个包装请求，将查询参数中的 token 放入 Authorization 头，
     * 使 TokenService.getLoginUser() 可正常解析。
     */
    private HttpServletRequest createRequestWrapper(HttpServletRequest original, String token) {
        return new jakarta.servlet.http.HttpServletRequestWrapper(original) {
            @Override
            public String getHeader(String name) {
                if ("Authorization".equalsIgnoreCase(name)) {
                    return "Bearer " + token;
                }
                return super.getHeader(name);
            }
        };
    }
}
