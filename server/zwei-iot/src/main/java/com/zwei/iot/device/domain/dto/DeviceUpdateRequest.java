package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 设备更新请求
 */
@Getter
@Setter
public class DeviceUpdateRequest {
    @NotBlank(message = "设备名称不能为空")
    private String name;

    private String sn;

    private Integer deviceType;

    private Integer networkType;

    private String protocolType;

    private String vendorName;

    private String icon;

    private String iconPath;

    @NotNull(message = "设备状态不能为空")
    private Integer status;
}
