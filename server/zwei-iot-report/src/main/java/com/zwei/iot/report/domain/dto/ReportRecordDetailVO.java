package com.zwei.iot.report.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 详情 VO (含 content) */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportRecordDetailVO extends ReportRecordVO {
    private String content;
}
