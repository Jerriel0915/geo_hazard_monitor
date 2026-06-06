package com.zwei.iot.monitor.domain.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 新增监测内容请求参数
 */
@Setter
@Getter
public class MonitorContentCreateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "监测类型ID不能为空")
    @Min(value = 1, message = "监测类型ID不合法")
    private Long monitorTypeId;

    @NotBlank(message = "监测内容编码不能为空")
    @Size(max = 100, message = "监测内容编码长度不能超过100个字符")
    private String code;

    @NotBlank(message = "监测内容名称不能为空")
    @Size(max = 200, message = "监测内容名称长度不能超过200个字符")
    private String name;

    @Size(max = 50, message = "单位长度不能超过50个字符")
    private String unit;

    @Size(max = 50, message = "指标类型长度不能超过50个字符")
    private String indicatorType;

    @Size(max = 200, message = "图标路径长度不能超过200个字符")
    private String icon;

    @Digits(integer = 10, fraction = 2, message = "最小值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMin;

    @Digits(integer = 10, fraction = 2, message = "最大值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMax;
}
