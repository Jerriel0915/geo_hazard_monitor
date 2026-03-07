package com.zwei.module.iot.mqtt.auth;

import com.zwei.iot.core.security.IDeviceAuthentication;
import com.zwei.module.iot.device.domain.Device;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerPublishPermission;
import org.springframework.stereotype.Service;
import org.tio.core.ChannelContext;

import javax.annotation.Resource;

/**
 * MQTT服务器发布权限验证器
 * 实现IMqttServerPublishValidator接口，用于验证设备发布权限
 * 
 * @author linx
 * @date 2025-09-05
 */
@Slf4j
@Service
public class MqttServerPublishValidator implements IMqttServerPublishPermission {

    @Resource
    private IDeviceAuthentication deviceAuthentication;

    /**
     * 验证设备发布权限
     */
    @Override
    public boolean hasPermission(ChannelContext context, String clientId, String topic, MqttQoS qoS, boolean isRetain) {
         // 从上下文中获取deviceKey
        Device device = (Device) context.get("device");
        String deviceKey = device.getDeviceKey();
            
        try {
            if (deviceKey == null) {
                log.warn("MQTT publish validate failed: deviceKey is null, clientId={}, topic={}", 
                        clientId, topic);
                return false;
            }
            
            // 检查发布权限
            boolean hasPermission = deviceAuthentication.checkPublishPermission(deviceKey, topic);
            
            if (!hasPermission) {
                log.warn("MQTT publish validate failed: permission denied, clientId={}, deviceKey={}, topic={}", 
                        clientId, deviceKey, topic);
            }
            
            return hasPermission;
        } catch (Exception e) {
            log.error("MQTT publish validate error: deviceKey={}, topic={}, error={}", 
                    deviceKey, topic, e.getMessage(), e);
            return false;
        }
    }
}