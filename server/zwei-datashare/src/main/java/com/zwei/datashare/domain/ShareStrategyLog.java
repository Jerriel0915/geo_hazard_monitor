package com.zwei.datashare.domain;

import com.zwei.datashare.enums.RunStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShareStrategyLog {

    private Long id;
    private Long strategyId;
    private LocalDateTime runTime;
    private RunStatus status;
    private String message;
    private Integer dataCount;
    private Integer duration;
    private LocalDateTime createTime;
}
