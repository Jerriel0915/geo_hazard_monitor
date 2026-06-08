package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DeviceMaintenanceRequest {
    @NotNull(message = "操作类型不能为空")
    private Integer operationType; // 1=报修, 2=修复, 3=停用, 4=恢复
    @NotBlank(message = "操作人不能为空")
    private String operatorName;
    private String operatorPhone;
    @NotBlank(message = "操作日期不能为空")
    private String operationDate; // yyyy-MM-dd HH:mm:ss
    private String description;
}
