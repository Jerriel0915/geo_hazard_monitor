package com.zwei.iot.device.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 设备注册请求
 */
@Getter
@Setter
public class DeviceRegisterRequest {
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    @NotBlank(message = "registerCode不能为空")
    private String registerCode;

    private String vendorName;

    @NotBlank(message = "sn不能为空")
    private String sn;

    @NotBlank(message = "deviceName不能为空")
    private String deviceName;

    @NotBlank(message = "deviceType不能为空")
    private String deviceType;

    @NotBlank(message = "network不能为空")
    private String network;

    @NotBlank(message = "protocol不能为空")
    private String protocol;

    @Valid
    @NotEmpty(message = "monitorTypes不能为空")
    private List<DeviceRegisterMonitorTypeRequest> monitorTypes;

    @Valid
    private List<DeviceRegisterChildDeviceRequest> childDevices;
}
