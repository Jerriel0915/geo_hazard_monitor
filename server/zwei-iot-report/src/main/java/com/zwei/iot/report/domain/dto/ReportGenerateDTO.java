package com.zwei.iot.report.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportGenerateDTO {
    @NotNull(message = "type 不能为空")
    private Integer type;

    @NotNull(message = "hazardPointId 不能为空")
    private Long hazardPointId;

    @NotNull(message = "periodStart 不能为空")
    private LocalDate periodStart;

    @NotNull(message = "periodEnd 不能为空")
    private LocalDate periodEnd;
}
