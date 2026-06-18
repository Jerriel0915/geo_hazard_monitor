package com.zwei.iot.report.domain.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportRecordPageDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private Integer type;
    private Long hazardPointId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer status;
    private String keyword;
}
