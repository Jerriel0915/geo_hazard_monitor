package com.zwei.iot.device.domain.brief;

import java.math.BigDecimal;

/**
 * 隐患点摘要 (供 report 等模块消费, 不暴露完整实体)。
 */
public record HazardPointBrief(
    Long id,
    String code,
    String name,
    BigDecimal longitude,
    BigDecimal latitude
) {}
