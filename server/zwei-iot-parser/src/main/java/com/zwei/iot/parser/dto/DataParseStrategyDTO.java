package com.zwei.iot.parser.dto;

import lombok.Data;
import java.util.List;

@Data
public class DataParseStrategyDTO {
    private Long id;
    private String name;
    private String sourceType;
    private String description;
    private Integer status;
    private String appScope;
    private String scriptCode;
    private List<Long> vendorIds;
    private List<Long> deviceIds;
}
