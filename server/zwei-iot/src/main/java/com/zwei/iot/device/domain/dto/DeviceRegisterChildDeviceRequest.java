package com.zwei.iot.device.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 本地组网子设备注册项
 */
@Getter
@Setter
public class DeviceRegisterChildDeviceRequest {
    @NotBlank(message = "子设备SN不能为空")
    private String sn;

    @NotBlank(message = "子设备名称不能为空")
    private String deviceName;

    @Valid
    @NotEmpty(message = "子设备监测类型不能为空")
    private List<DeviceRegisterMonitorTypeRequest> monitorTypes;
}
