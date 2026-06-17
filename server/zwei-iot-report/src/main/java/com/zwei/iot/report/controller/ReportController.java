package com.zwei.iot.report.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.domain.dto.ReportGenerateAllDTO;
import com.zwei.iot.report.domain.dto.ReportGenerateDTO;
import com.zwei.iot.report.domain.dto.ReportRecordDetailVO;
import com.zwei.iot.report.domain.dto.ReportRecordPageDTO;
import com.zwei.iot.report.domain.dto.ReportRecordVO;
import com.zwei.iot.report.service.ReportGenerationService;
import com.zwei.iot.report.service.ReportRecordService;
import com.zwei.iot.report.support.ReportPeriod;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 报告管理 REST 接口。
 */
@RestController
@RequestMapping("/api/v1/report/records")
public class ReportController extends BaseController {

    private final ReportRecordService recordService;
    private final ReportGenerationService generationService;
    private final IHazardPointQueryService hazardQuery;

    public ReportController(ReportRecordService recordService,
                             ReportGenerationService generationService,
                             IHazardPointQueryService hazardQuery) {
        this.recordService = recordService;
        this.generationService = generationService;
        this.hazardQuery = hazardQuery;
    }

    @PreAuthorize("@ss.hasPermi('report:record:list')")
    @GetMapping("/page")
    public AjaxResult page(ReportRecordPageDTO params) {
        PageResult<ReportRecordVO> result = recordService.page(params);
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('report:record:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        ReportRecordDetailVO vo = recordService.detail(id);
        if (vo == null) return error("报告不存在");
        return AjaxResult.success(vo);
    }

    @PreAuthorize("@ss.hasPermi('report:record:remove')")
    @Log(title = "报告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return recordService.remove(id) ? success() : error("删除失败");
    }

    @PreAuthorize("@ss.hasPermi('report:record:generate')")
    @Log(title = "报告管理", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@Valid @RequestBody ReportGenerateDTO dto) {
        if (dto.getType() == null || (dto.getType() != 2 && dto.getType() != 3 && dto.getType() != 4)) {
            return AjaxResult.error("type 必须为 2(周报)/3(月报)/4(季报)");
        }
        if (dto.getPeriodEnd().isBefore(dto.getPeriodStart())) {
            return AjaxResult.error("periodEnd 必须 >= periodStart");
        }
        long days = ChronoUnit.DAYS.between(dto.getPeriodStart(), dto.getPeriodEnd());
        if (days > 400) {
            return AjaxResult.error("周期跨度不能超过 400 天");
        }

        List<HazardPointBrief> all = hazardQuery.listMonitoring();
        HazardPointBrief hp = all.stream()
            .filter(h -> h.id().equals(dto.getHazardPointId()))
            .findFirst()
            .orElse(null);

        Long existingId = recordService.findExisting(dto.getType(), dto.getHazardPointId(),
            dto.getPeriodStart(), dto.getPeriodEnd());
        if (existingId != null) {
            return AjaxResult.error(409, "该周期报告已存在").put("reportId", existingId);
        }

        ReportType type = ReportType.fromCode(dto.getType());
        ReportPeriod period = new ReportPeriod(dto.getPeriodStart(), dto.getPeriodEnd());
        HazardPointBrief finalHp = hp != null ? hp
            : new HazardPointBrief(dto.getHazardPointId(), null, null, null, null);
        try {
            generationService.generateOne(type, period, finalHp);
        } catch (Exception e) {
            return AjaxResult.error("生成失败: " + e.getMessage());
        }
        Long newId = recordService.findExisting(dto.getType(), dto.getHazardPointId(),
            dto.getPeriodStart(), dto.getPeriodEnd());
        return AjaxResult.success("生成成功").put("reportId", newId);
    }

    @PreAuthorize("@ss.hasPermi('report:record:generate')")
    @Log(title = "报告管理-批量生成", businessType = BusinessType.INSERT)
    @PostMapping("/generate-all")
    public AjaxResult generateAll(@Valid @RequestBody ReportGenerateAllDTO dto) {
        if (dto.getType() == null || (dto.getType() != 2 && dto.getType() != 3 && dto.getType() != 4)) {
            return AjaxResult.error("type 必须为 2(周报)/3(月报)/4(季报)");
        }
        LocalDate refDate = dto.getReferenceDate() != null ? dto.getReferenceDate() : LocalDate.now();
        ReportType type = ReportType.fromCode(dto.getType());
        // 定时任务逻辑复用的核心：同一方法，按参考日期计算周期
        generationService.generateAll(type, refDate);
        return AjaxResult.success("批量生成已触发");
    }
}
