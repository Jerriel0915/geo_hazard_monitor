package com.zwei.iot.device.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
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

    /**
     * 埋深(米)，地面为0点，向下为正，向上为负
     */
    @Digits(integer = 8, fraction = 2, message = "埋深最多支持8位整数和2位小数")
    private BigDecimal burialDepth;

    @Valid
    @NotEmpty(message = "属性列表不能为空")
    private List<SensorAttributeRequest> attrList;
}
