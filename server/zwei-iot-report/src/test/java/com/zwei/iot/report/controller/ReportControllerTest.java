package com.zwei.iot.report.controller;

import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.domain.dto.ReportGenerateDTO;
import com.zwei.iot.report.service.ReportGenerationService;
import com.zwei.iot.report.service.ReportRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportController")
class ReportControllerTest {

    private ReportController controller;
    private ReportRecordService recordService;
    private ReportGenerationService generationService;
    private IHazardPointQueryService hazardQuery;

    @BeforeEach
    void setUp() {
        recordService = mock(ReportRecordService.class);
        generationService = mock(ReportGenerationService.class);
        hazardQuery = mock(IHazardPointQueryService.class);
        controller = new ReportController(recordService, generationService, hazardQuery);
    }

    @Test
    @DisplayName("generate 校验 type 不合法返回 error msg")
    void generateInvalidType() {
        ReportGenerateDTO dto = buildDTO(9, 1L, "2026-06-01", "2026-06-07");
        AjaxResult r = controller.generate(dto);
        assertThat(r.get("code")).isEqualTo(500);
        assertThat(r.get("msg").toString()).contains("type");
    }

    @Test
    @DisplayName("generate period 反向被拒")
    void generatePeriodReversed() {
        ReportGenerateDTO dto = buildDTO(2, 1L, "2026-06-07", "2026-06-01");
        AjaxResult r = controller.generate(dto);
        assertThat(r.get("code")).isEqualTo(500);
        assertThat(r.get("msg").toString()).contains("periodEnd");
    }

    @Test
    @DisplayName("generate 同周期已存在返回 409 + reportId")
    void generateConflict() {
        when(hazardQuery.listMonitoring()).thenReturn(List.of(
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"))));
        when(recordService.findExisting(eq(2), eq(1L), any(), any())).thenReturn(999L);

        ReportGenerateDTO dto = buildDTO(2, 1L, "2026-06-01", "2026-06-07");
        AjaxResult r = controller.generate(dto);
        assertThat(r.get("code")).isEqualTo(409);
        assertThat(r.get("reportId")).isEqualTo(999L);
    }

    @Test
    @DisplayName("generate 成功调用 generationService")
    void generateSuccess() throws Exception {
        when(hazardQuery.listMonitoring()).thenReturn(List.of(
            new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"))));
        when(recordService.findExisting(eq(2), eq(1L), any(), any())).thenReturn(null).thenReturn(100L);
        doNothing().when(generationService).generateOne(any(), any(), any());

        ReportGenerateDTO dto = buildDTO(2, 1L, "2026-06-01", "2026-06-07");
        AjaxResult r = controller.generate(dto);
        assertThat(r.get("code")).isEqualTo(200);
        assertThat(r.get("msg").toString()).contains("生成成功");
        assertThat(r.get("reportId")).isEqualTo(100L);
    }

    @Test
    @DisplayName("detail 返回 null 时报报告不存在")
    void detailNotFound() {
        when(recordService.detail(999L)).thenReturn(null);
        AjaxResult r = controller.detail(999L);
        assertThat(r.get("code")).isEqualTo(500);
        assertThat(r.get("msg").toString()).contains("报告不存在");
    }

    @Test
    @DisplayName("remove 成功返回 success")
    void removeSuccess() {
        when(recordService.remove(1L)).thenReturn(true);
        AjaxResult r = controller.remove(1L);
        assertThat(r.get("code")).isEqualTo(200);
    }

    @Test
    @DisplayName("remove 失败返回 error")
    void removeFail() {
        when(recordService.remove(1L)).thenReturn(false);
        AjaxResult r = controller.remove(1L);
        assertThat(r.get("code")).isEqualTo(500);
        assertThat(r.get("msg").toString()).contains("删除失败");
    }

    @Test
    @DisplayName("generate 跨度超400天被拒")
    void generateSpanTooLarge() {
        ReportGenerateDTO dto = buildDTO(2, 1L, "2025-01-01", "2026-06-01");
        AjaxResult r = controller.generate(dto);
        assertThat(r.get("code")).isEqualTo(500);
        assertThat(r.get("msg").toString()).contains("400");
    }

    private static ReportGenerateDTO buildDTO(int type, Long hpId, String start, String end) {
        ReportGenerateDTO dto = new ReportGenerateDTO();
        dto.setType(type);
        dto.setHazardPointId(hpId);
        dto.setPeriodStart(LocalDate.parse(start));
        dto.setPeriodEnd(LocalDate.parse(end));
        return dto;
    }
}
