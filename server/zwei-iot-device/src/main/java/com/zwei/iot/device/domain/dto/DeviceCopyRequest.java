package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备复制请求
 */
@Getter
@Setter
public class DeviceCopyRequest {
    @NotBlank(message = "设备编号不能为空")
    private String code;

    @NotBlank(message = "设备名称不能为空")
    private String name;

    /** 是否同时复制传感器，默认 true（向后兼容） */
    private Boolean copySensors = true;
}
