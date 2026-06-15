package com.zwei.iot.parser.domain;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DataParseLog {
    private Long id;
    private Long strategyId;
    private String logLevel;
    private String message;
    private String data;
    private String topic;
    private String deviceCode;
    private String parseResult;
    private Integer executionTime;
    private String errorStack;
    private java.util.Date createTime;
}
