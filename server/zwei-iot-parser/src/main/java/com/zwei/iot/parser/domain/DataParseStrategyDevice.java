package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseStrategyDevice {
    private Long id;
    private Long strategyId;
    private Long deviceId;
}
