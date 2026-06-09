package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 修改隐患点请求参数
 */
@Setter
@Getter
public class HazardPointUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "隐患点名称不能为空")
    @Size(max = 200, message = "隐患点名称长度不能超过200个字符")
    private String name;

    private Long groupId;

    @DecimalMin(value = "-180.000000", message = "经度不能小于-180")
    @DecimalMax(value = "180.000000", message = "经度不能大于180")
    @Digits(integer = 3, fraction = 6, message = "经度最多支持6位小数")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.000000", message = "纬度不能小于-90")
    @DecimalMax(value = "90.000000", message = "纬度不能大于90")
    @Digits(integer = 2, fraction = 6, message = "纬度最多支持6位小数")
    private BigDecimal latitude;

    @DecimalMin(value = "0.00", message = "走向角度不能小于0")
    @DecimalMax(value = "360.00", message = "走向角度不能大于360")
    @Digits(integer = 3, fraction = 2, message = "走向角度最多支持2位小数")
    private BigDecimal strike;

    /**
     * 边界范围 JSON
     */
    private String boundaryCoords;

    @Size(max = 65535, message = "隐患描述长度超出限制")
    private String description;
}
