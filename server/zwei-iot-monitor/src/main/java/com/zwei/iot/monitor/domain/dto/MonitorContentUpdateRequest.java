package com.zwei.iot.monitor.domain.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 修改监测内容请求参数
 */
@Setter
@Getter
public class MonitorContentUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Pattern(regexp = ".*\\S.*", message = "监测内容名称不能为空")
    @Size(max = 200, message = "监测内容名称长度不能超过200个字符")
    private String name;

    @Pattern(regexp = ".*\\S.*", message = "单位不能为空")
    @Size(max = 50, message = "单位长度不能超过50个字符")
    private String unit;

    private Integer sortOrder;

    @Pattern(regexp = ".*\\S.*", message = "图标路径不能为空")
    @Size(max = 200, message = "图标路径长度不能超过200个字符")
    private String icon;

    @Digits(integer = 10, fraction = 2, message = "最小值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMin;

    @Digits(integer = 10, fraction = 2, message = "最大值范围最多支持10位整数和2位小数")
    private BigDecimal rangeMax;

    public boolean hasUpdatableField() {
        return name != null || unit != null || sortOrder != null || icon != null || rangeMin != null || rangeMax != null;
    }
}
