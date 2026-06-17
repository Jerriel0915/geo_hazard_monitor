package com.zwei.iot.report.domain;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报告记录实体 (对应 report_record 表)。
 */
@Data
public class ReportRecord {

    private Long id;
    private Long templateId;
    private String templateName;

    /** 报告类型 2-周报 3-月报 4-季报 (对应 {@link ReportType#code()}) */
    private Integer type;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    private Long hazardPointId;
    private String hazardPointCode;
    private String hazardPointName;

    private String reportName;
    private LocalDateTime reportDate;

    /** HTML 内容 (列表查询不返回,详情查询返回) */
    private String content;

    private String filePath;

    /** 状态 1-生成中 2-已生成 3-生成失败 */
    private Integer status;

    private String errorMsg;

    private Integer delFlag;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
