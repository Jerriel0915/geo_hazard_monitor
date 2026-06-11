package com.zwei.iot.hazardpoint.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * 隐患点 boundary_coords JSON 的强类型镜像。
 * wire 格式使用数组 [lat,lng]。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoundaryCoordsDTO(
    List<List<BigDecimal>> polygon,
    List<List<BigDecimal>> strikeLine,
    List<List<List<BigDecimal>>> auxiliaryLines
) {
    public static BoundaryCoordsDTO empty() {
        return new BoundaryCoordsDTO(List.of(), null, List.of());
    }
}
