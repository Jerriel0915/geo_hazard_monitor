package com.zwei.iot.parser.dto;

import lombok.Data;
import java.util.Map;

@Data
public class DataParseTestResponse {
    private boolean success;
    private long executionTime;
    private Map<String, Object> parseResult;
    private String error;
}
