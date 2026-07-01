package com.zwei.iot.parser.dto;

import lombok.Data;

@Data
public class DataParseStrategyQueryDTO {
    /** 关键字（name 或 topic 模糊匹配） */
    private String keyword;
    private String name;
    private String sourceType;
    private Integer status;
    private String appScope;
    private String topic;
}
