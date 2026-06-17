package com.zwei.iot.report.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

/**
 * 批量生成请求 (一键生成)。
 * <p>
 * 参考日期为空时默认当天，生成为上一个完整周期的报告（与定时任务逻辑一致）。
 */
@Data
public class ReportGenerateAllDTO {
    @NotNull(message = "type 不能为空")
    private Integer type;

    /** 可选：参考日期，默认当天 */
    private LocalDate referenceDate;
}
