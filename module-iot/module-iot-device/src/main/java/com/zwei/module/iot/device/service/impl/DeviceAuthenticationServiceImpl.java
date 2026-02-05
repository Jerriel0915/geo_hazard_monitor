package com.zwei.module.iot.device.service.impl;

import com.zwei.iot.core.security.IDeviceAuthentication;
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 设备认证实现类
 *
 * @author linx
 * @date 2025-09-05
 */
@Slf4j
@Service
public class DeviceAuthenticationServiceImpl implements IDeviceAuthentication {

    @Autowired
    private DeviceMapper deviceMapper;

    /**
     * 设备认证
     * 使用deviceKey和deviceSecret进行认证
     * 两个参数均不可为空
     */
    @Override
    public boolean authenticate(String deviceKey, String deviceSecret) {
        try {
            // 参数校验
            if (deviceKey == null || deviceSecret == null || deviceKey.isEmpty() || deviceSecret.isEmpty()) {
                log.warn("Device authentication failed: invalid parameters");
                return false;
            }

            // 创建查询条件
            Device queryDevice = Device.builder()
                    .deviceKey(deviceKey)
                    .deviceSecret(deviceSecret).build();

            // 查询设备
            Device device = deviceMapper.selectDeviceList(queryDevice).stream().findFirst().orElse(null);

            if (device != null) {
                log.info("Device authentication success: deviceKey={}", deviceKey);
                return true;
            } else {
                log.warn("Device authentication failed: device not found or invalid credentials, deviceKey={}", deviceKey);
                return false;
            }
        } catch (Exception e) {
            log.error("Device authentication error: deviceKey={}, error={}", deviceKey, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查设备是否有权限订阅主题
     * 默认设备只能订阅对应产品下的设备的主题，格式如：/device/${productKey}/${deviceKey}/#
     */
    @Override
    public boolean checkSubscribePermission(String deviceKey, String topic) {
        try {
            if (deviceKey == null || topic == null) {
                return false;
            }

            // 构建设备可访问的基础主题前缀
            Device device = Device.builder().deviceKey(deviceKey).build();
            String productKey = deviceMapper.selectDeviceList(device).stream().findFirst().map(Device::getDeviceKey).orElse(null);

            if (productKey == null) {
                log.warn("Subscribe permission denied: deviceKey={}, topic={}", deviceKey, topic);
                return false;
            }

            String allowedTopicPrefix = "/device/" + productKey + "/" + deviceKey;

            // 检查主题是否以设备的专属前缀开头
            boolean hasPermission = topic.startsWith(allowedTopicPrefix + "/") || topic.equals(allowedTopicPrefix);

            if (!hasPermission) {
                log.warn("Subscribe permission denied: productKey={}, deviceKey={}, topic={}", productKey, deviceKey, topic);
            } else {
                log.debug("Subscribe permission granted: productKey={}, deviceKey={}, topic={}", productKey, deviceKey, topic);
            }

            return hasPermission;
        } catch (Exception e) {
            log.error("Check subscribe permission error: deviceKey={}, topic={}, error={}",
                    deviceKey, topic, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查设备是否有权限发布消息
     * 默认设备只能发布到自己的主题，格式如：/device/${productKey}/${deviceKey}/#
     */
    @Override
    public boolean checkPublishPermission(String deviceKey, String topic) {
        try {
            if (deviceKey == null || topic == null) {
                return false;
            }

            // 构建设备可访问的基础主题前缀
            Device device = Device.builder().deviceKey(deviceKey).build();
            String productKey = deviceMapper.selectDeviceList(device).stream().findFirst().map(Device::getDeviceKey).orElse(null);

            if (productKey == null) {
                log.warn("Publish permission denied: deviceKey={}, topic={}", deviceKey, topic);
                return false;
            }

            String allowedTopicPrefix = "/device/" + productKey + "/" + deviceKey;

            // 检查主题是否以设备的专属前缀开头
            boolean hasPermission = topic.startsWith(allowedTopicPrefix + "/") || topic.equals(allowedTopicPrefix);

            if (!hasPermission) {
                log.warn("Publish permission denied: productKey={}, deviceKey={}, topic={}", productKey, deviceKey, topic);
            } else {
                log.debug("Publish permission granted: productKey={}, deviceKey={}, topic={}", productKey, deviceKey, topic);
            }

            return hasPermission;
        } catch (Exception e) {
            log.error("Check publish permission error: deviceKey={}, topic={}, error={}",
                    deviceKey, topic, e.getMessage(), e);
            return false;
        }
    }
}