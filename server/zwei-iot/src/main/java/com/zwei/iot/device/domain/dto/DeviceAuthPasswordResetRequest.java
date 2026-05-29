package com.zwei.iot.device.domain.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 设备密码重置请求
 */
@Getter
@Setter
public class DeviceAuthPasswordResetRequest {
    private String reason;

    private Boolean forceOffline;
}
