package com.zwei.datashare.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareStrategyScript {

    private Long id;
    private Long strategyId;
    private String script;
    private String variables;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
