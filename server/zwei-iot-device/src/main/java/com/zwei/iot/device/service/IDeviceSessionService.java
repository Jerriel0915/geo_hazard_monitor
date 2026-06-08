package com.zwei.iot.device.service;

/**
 * 设备 MQTT 会话管理服务接口（跨模块）。
 * <p>
 * 接口定义在 zwei-iot-device，实现在 zwei-iot-broker 模块中，
 * 用于支持设备管理侧触发 MQTT 会话级别的操作（如密码重置后强制断连）。
 *
 * @author Jerriel
 * @date: 2026-06-08
 */
public interface IDeviceSessionService {

    /**
     * 强制断开设备的 MQTT 连接。
     * <p>
     * 若设备当前无活跃会话，视为已断开，返回 {@code true}。
     *
     * @param deviceId 设备主键
     * @return true 表示设备已处于未连接状态（含成功断开）
     */
    boolean disconnectDevice(Long deviceId);
}
