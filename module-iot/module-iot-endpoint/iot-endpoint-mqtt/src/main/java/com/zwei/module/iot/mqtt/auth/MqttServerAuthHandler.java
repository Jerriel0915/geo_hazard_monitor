package com.zwei.module.iot.mqtt.auth;

import com.zwei.iot.core.security.IDeviceAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;

import javax.annotation.Resource;

/**
 * MQTT服务器认证处理器
 * 实现IMqttServerAuthHandler接口，用于验证设备登录认证
 * 
 * @author linx
 * @date 2025-09-05
 */
@Slf4j
@Service
public class MqttServerAuthHandler implements IMqttServerAuthHandler {

    @Resource
    private IDeviceAuthentication deviceAuthentication;

    /**
     * MQTT认证方法
     * 使用deviceKey和deviceSecret进行设备认证
     */
    @Override
    public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        try {
            log.debug("MQTT authenticate: clientId={}, userName={}", clientId, username);
            
            // 对于MQTT，通常userName作为deviceKey，password作为deviceSecret
            if (username == null || password == null) {
                log.warn("MQTT authenticate failed: missing credentials, clientId={}", clientId);
                return false;
            }
            
            // 使用设备认证服务进行认证
            boolean authenticated = deviceAuthentication.authenticate(username, password);
            
            if (!authenticated) {
                log.warn("MQTT authenticate failed: invalid credentials, clientId={}, deviceKey={}", 
                        clientId, username);
            } else {
                log.info("MQTT authenticate success: clientId={}, deviceKey={}", clientId, username);
                // 将deviceKey存储到上下文中，便于后续权限验证
                context.set("deviceKey", username);
            }
            
            return authenticated;
        } catch (Exception e) {
            log.error("MQTT authenticate error: clientId={}, userName={}, error={}", 
                    clientId, username, e.getMessage(), e);
            return false;
        }
    }
}