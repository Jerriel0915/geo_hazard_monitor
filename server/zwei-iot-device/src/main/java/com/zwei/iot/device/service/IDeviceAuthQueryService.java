package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.Device;

public interface IDeviceAuthQueryService {
    Device findByAuthUsername(String authUsername);

    /**
     * 更新设备认证时间戳与接入IP（替代过宽的 updateDevice(Device)）。
     *
     * @param deviceId      设备ID
     * @param lastAuthTime  最近认证时间
     * @param lastAuthIp    最近认证IP
     */
    void updateAuthInfo(Long deviceId, String lastAuthTime, String lastAuthIp);
}
