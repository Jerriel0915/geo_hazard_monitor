package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备注册监测类型项
 */
@Getter
@Setter
public class DeviceRegisterMonitorTypeRequest {
    @NotBlank(message = "监测类型编码不能为空")
    private String type;

    @NotBlank(message = "传感器编号不能为空")
    private String sid;
}
