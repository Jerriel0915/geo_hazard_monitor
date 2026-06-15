package com.zwei.iot.parser.dto;

import lombok.Data;

@Data
public class DataParseStrategyQueryDTO {
    private String name;
    private String sourceType;
    private Integer status;
    private String appScope;
}
