package com.zwei.iot.device.domain.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 传感器属性请求参数
 */
@Setter
@Getter
public class SensorAttributeRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long monitorContentId;

    @NotBlank(message = "属性编码不能为空")
    @Size(max = 100, message = "属性编码长度不能超过100个字符")
    private String attrCode;

    @NotBlank(message = "属性名称不能为空")
    @Size(max = 200, message = "属性名称长度不能超过200个字符")
    private String attrName;

    @Size(max = 50, message = "指标类型长度不能超过50个字符")
    private String indicatorType;

    @Size(max = 100, message = "指标类型名称长度不能超过100个字符")
    private String indicatorTypeName;

    @Digits(integer = 10, fraction = 2, message = "初始值最多支持10位整数和2位小数")
    private BigDecimal initialValue;

    @Size(max = 50, message = "单位长度不能超过50个字符")
    private String unit;

    @Digits(integer = 10, fraction = 2, message = "最小值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMin;

    @Digits(integer = 10, fraction = 2, message = "最大值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMax;

    @Size(max = 500, message = "图标路径长度不能超过500个字符")
    private String icon;
}
