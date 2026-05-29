package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备账号状态变更请求
 */
@Getter
@Setter
public class DeviceAuthStatusChangeRequest {
    @NotNull(message = "账号状态不能为空")
    private Integer authStatus;

    private String reason;
}
