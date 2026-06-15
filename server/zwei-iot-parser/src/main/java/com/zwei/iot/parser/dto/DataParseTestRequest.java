package com.zwei.iot.parser.dto;

import lombok.Data;

@Data
public class DataParseTestRequest {
    private Long strategyId;
    private String scriptCode;
    private String topic;
    private String testData;
}
