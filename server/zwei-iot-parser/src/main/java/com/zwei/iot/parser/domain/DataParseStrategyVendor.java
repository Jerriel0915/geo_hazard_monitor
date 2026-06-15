package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategyVendor {
    private Long id;
    private Long strategyId;
    private Long vendorId;
}
