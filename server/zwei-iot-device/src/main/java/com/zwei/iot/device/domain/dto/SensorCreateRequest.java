package com.zwei.iot.device.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增传感器请求参数
 */
@Setter
@Getter
public class SensorCreateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "传感器编号不能为空")
    @Size(max = 100, message = "传感器编号长度不能超过100个字符")
    private String sensorCode;

    /**
     * 传感器主题编号（可选）。
     * <p>
     * 留空时后端 {@code DeviceSensorServiceImpl.fillDeviceFields} 会用 {@code sensorCode} 兜底。
     * 前端通常按规则 {@code {indicator_type(大写)}_{nextId}} 自动预填。
     * 同一设备下唯一，由 {@code uk_device_sensor_no} 索引兜底。
     */
    @Size(max = 32, message = "主题编号长度不能超过32个字符")
    private String sensorNo;

    @NotBlank(message = "传感器名称不能为空")
    @Size(max = 200, message = "传感器名称长度不能超过200个字符")
    private String sensorName;

    @NotNull(message = "监测类型ID不能为空")
    @Min(value = 1, message = "监测类型ID不合法")
    private Long monitorTypeId;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;

    @Valid
    @NotEmpty(message = "属性列表不能为空")
    private List<SensorAttributeRequest> attrList;
}
