package com.zwei.common.domain;

import java.io.Serial;
import java.io.Serializable;

/**
 * TSL 属性运行时值 — parser 模块解析产出的最小数据单元。
 *
 * <p>每个 PropertyValue 对应 TSL properties 数组中的一个属性，
 * 包含标识符、名称、单位、运行时数值和质量码。
 *
 * @param identifier TslProperty.identifier（如 "rainfall_hour"）
 * @param name       TslProperty.name（中文名称）
 * @param unit       计量单位（如 "mm"）
 * @param value      运行时数值
 * @param quality    质量码（0=正常，非零=异常）
 */
public record PropertyValue(
        String identifier,
        String name,
        String unit,
        Double value,
        Integer quality
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
