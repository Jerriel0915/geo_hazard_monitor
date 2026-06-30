package com.zwei.terra.agent.duty;

import com.zwei.common.constant.CacheConstants;
import com.zwei.common.constant.Constants;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.utils.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 值守模式 WebSocket 握手拦截器 — 从 query param 提取 JWT 并验证。
 *
 * <p>WebSocket 浏览器 API 不支持自定义请求头，因此 token 通过
 * {@code ws://host/ws/terramens/duty?token=xxx} 查询参数传递。</p>
 *
 * <p>验证通过后，将 userId 和 LoginUser 存入 WebSocket session attributes，
 * 后续 TerraDutyWebSocketHandler 可直接使用。</p>
 */
@Component
@Slf4j
public class TerraDutyHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private RedisCache redisCache;

    @Value("${token.secret}")
    private String secret;

    /** WebSocket session attributes 中存储 userId 的 key */
    public static final String ATTR_USER_ID = "dutyUserId";
    public static final String ATTR_USERNAME = "dutyUsername";
    public static final String ATTR_LOGIN_USER = "dutyLoginUser";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            log.warn("值守模式 WebSocket 握手失败: 非 Servlet 请求");
            return false;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = httpRequest.getParameter("token");

        if (StringUtils.isEmpty(token)) {
            log.warn("值守模式 WebSocket 握手失败: 缺少 token 参数");
            return false;
        }

        try {
            // 解析 JWT
            Claims claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();

            String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
            String userKey = CacheConstants.LOGIN_TOKEN_KEY + uuid;
            LoginUser loginUser = redisCache.getCacheObject(userKey);

            if (loginUser == null) {
                log.warn("值守模式 WebSocket 握手失败: token 已过期或无效");
                return false;
            }

            // 将用户信息存入 WebSocket session
            attributes.put(ATTR_USER_ID, loginUser.getUserId());
            attributes.put(ATTR_USERNAME, loginUser.getUsername());
            attributes.put(ATTR_LOGIN_USER, loginUser);

            log.info("值守模式 WebSocket 握手成功: userId={}, username={}",
                    loginUser.getUserId(), loginUser.getUsername());
            return true;

        } catch (Exception e) {
            log.warn("值守模式 WebSocket 握手失败: token 解析异常: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // 无需后处理
    }
}
