package com.zwei.iot.report.domain.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 列表项 VO (不含 content 大字段) */
@Data
public class ReportRecordVO {
    private Long id;
    private Integer type;
    private String typeDesc;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Long hazardPointId;
    private String hazardPointCode;
    private String hazardPointName;
    private String reportName;
    private Integer status;
    private String statusDesc;
    private String errorMsg;
    private LocalDateTime createTime;
}
