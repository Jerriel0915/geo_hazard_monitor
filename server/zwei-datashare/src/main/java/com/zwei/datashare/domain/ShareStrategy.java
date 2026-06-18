package com.zwei.datashare.domain;

import com.zwei.datashare.enums.ScopeType;
import com.zwei.datashare.enums.ShareMethod;
import com.zwei.datashare.enums.StrategyStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareStrategy {

    private Long id;
    private String code;
    private String name;
    private String description;
    private ShareMethod method;
    private String address;
    private String topic;
    private String username;
    private String password;
    private String params;
    private ScopeType scopeType;
    private String scopeIds;
    private String cron;
    private StrategyStatus status;
    private Integer successCount;
    private LocalDateTime lastRunTime;
    private String lastRunStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
