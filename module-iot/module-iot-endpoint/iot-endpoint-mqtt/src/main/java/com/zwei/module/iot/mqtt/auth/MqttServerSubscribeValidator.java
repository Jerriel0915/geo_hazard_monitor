package com.zwei.module.iot.mqtt.auth;

import com.zwei.iot.core.security.IDeviceAuthentication;
import lombok.extern.slf4j.Slf4j;

import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;

import javax.annotation.Resource;

/**
 * MQTT服务器订阅权限验证器
 * 实现IMqttServerSubscribeValidator接口，用于验证设备订阅权限
 * 
 * @author linx
 * @date 2025-09-05
 */
@Slf4j
@Service
public class MqttServerSubscribeValidator implements IMqttServerSubscribeValidator {

    @Resource
    private IDeviceAuthentication deviceAuthentication;

    /**
     * 验证设备订阅权限
     */
    @Override
    public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
        // 从上下文中获取deviceKey
        String deviceKey = (String) context.get("deviceKey");
        try {
            
            if (deviceKey == null) {
                log.warn("MQTT subscribe validate failed: deviceKey is null, clientId={}, topic={}", 
                        clientId, topicFilter);
                return false;
            }
            
            // 检查订阅权限
            boolean hasPermission = deviceAuthentication.checkSubscribePermission(deviceKey, topicFilter);
            
            if (!hasPermission) {
                log.warn("MQTT subscribe validate failed: permission denied, deviceKey={}, topic={}", 
                        deviceKey, topicFilter);
            }
            
            return hasPermission;
        } catch (Exception e) {
            log.error("MQTT subscribe validate error: deviceKey={}, topic={}, error={}", 
                    deviceKey, topicFilter, e.getMessage(), e);
            return false;
        }
    }
}