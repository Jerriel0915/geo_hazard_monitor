package com.zwei.iot.report.service;

import com.zwei.common.redis.DistributedLock;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.report.datasource.ReportContext;
import com.zwei.iot.report.datasource.ReportDataAssembler;
import com.zwei.iot.report.domain.ReportRecord;
import com.zwei.iot.report.domain.ReportType;
import com.zwei.iot.report.mapper.ReportRecordMapper;
import com.zwei.iot.report.render.WeeklyReportRenderer;
import com.zwei.iot.report.support.ReportPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ReportGenerationService")
class ReportGenerationServiceTest {

    private ReportRecordMapper mapper;
    private ReportDataAssembler assembler;
    private IHazardPointQueryService hazardQuery;
    private DistributedLock lock;
    private WeeklyReportRenderer renderer;
    private ReportGenerationService service;

    @BeforeEach
    void setUp() {
        mapper = mock(ReportRecordMapper.class);
        assembler = mock(ReportDataAssembler.class);
        hazardQuery = mock(IHazardPointQueryService.class);
        lock = mock(DistributedLock.class);
        renderer = mock(WeeklyReportRenderer.class);
        service = new ReportGenerationService(mapper, assembler, hazardQuery, lock, List.of(renderer));
    }

    @Test
    @DisplayName("Redis 锁获取失败时跳过整批")
    void skipWhenLockFails() {
        when(lock.tryLock(anyString(), any(Duration.class)))
            .thenReturn(DistributedLock.LockToken.notAcquired());

        service.generateAll(ReportType.WEEKLY);

        verifyNoInteractions(hazardQuery, assembler);
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("已存在成功记录的 hp 被跳过")
    void skipExisting() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp));
        ReportPeriod period = ReportPeriod.lastWeek(LocalDate.now());
        when(mapper.selectExistingSuccess(eq(2), eq(1L), eq(period.start()), eq(period.end())))
            .thenReturn(new ReportRecord());

        service.generateAll(ReportType.WEEKLY);

        verify(assembler, never()).build(any(), any(), any());
        verify(mapper, never()).insert(any());
    }

    @Test
    @DisplayName("正常流程: insert 占位 -> assemble -> render -> updateStatus")
    void happyPath() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp = new HazardPointBrief(1L, "HP001", "测试", new BigDecimal("104"), new BigDecimal("30"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp));
        when(mapper.selectExistingSuccess(anyInt(), anyLong(), any(), any())).thenReturn(null);
        // mock insert 回填 id (模拟 useGeneratedKeys)
        when(mapper.insert(any())).thenAnswer(invocation -> {
            ReportRecord r = invocation.getArgument(0);
            r.setId(100L);
            return 1;
        });

        ReportContext ctx = mock(ReportContext.class);
        when(ctx.type()).thenReturn(ReportType.WEEKLY);
        when(ctx.period()).thenReturn(new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)));
        when(ctx.hazardPoint()).thenReturn(hp);
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp))).thenReturn(ctx);
        when(renderer.type()).thenReturn(ReportType.WEEKLY);
        when(renderer.render(ctx)).thenReturn("<html>...</html>");

        service.generateAll(ReportType.WEEKLY);

        ArgumentCaptor<ReportRecord> captor = ArgumentCaptor.forClass(ReportRecord.class);
        verify(mapper).insert(captor.capture());
        ReportRecord inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo(1);
        verify(mapper).updateStatusAndContent(eq(100L), eq(2), eq("<html>...</html>"), isNull());
    }

    @Test
    @DisplayName("单 hp 渲染异常时记 status=3 + error_msg, 继续下一个")
    void singleFailureIsolated() {
        when(lock.tryLock(anyString(), any())).thenReturn(new DistributedLock.LockToken(true, "t1"));
        HazardPointBrief hp1 = new HazardPointBrief(1L, "HP001", "测试1", new BigDecimal("104"), new BigDecimal("30"));
        HazardPointBrief hp2 = new HazardPointBrief(2L, "HP002", "测试2", new BigDecimal("105"), new BigDecimal("31"));
        when(hazardQuery.listMonitoring()).thenReturn(List.of(hp1, hp2));
        when(mapper.selectExistingSuccess(anyInt(), anyLong(), any(), any())).thenReturn(null);
        when(mapper.insert(any())).thenAnswer(invocation -> {
            ReportRecord r = invocation.getArgument(0);
            r.setId(r.getHazardPointId()); // 用 hpId 模拟唯一 id
            return 1;
        });
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp1)))
            .thenThrow(new RuntimeException("IoTDB down"));
        ReportContext ctx2 = mock(ReportContext.class);
        when(ctx2.type()).thenReturn(ReportType.WEEKLY);
        when(ctx2.period()).thenReturn(new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14)));
        when(ctx2.hazardPoint()).thenReturn(hp2);
        when(assembler.build(eq(ReportType.WEEKLY), any(), eq(hp2))).thenReturn(ctx2);
        when(renderer.type()).thenReturn(ReportType.WEEKLY);
        when(renderer.render(any())).thenReturn("<html>hp2</html>");

        service.generateAll(ReportType.WEEKLY);

        verify(mapper, atLeastOnce()).updateStatusAndContent(anyLong(), eq(3), isNull(), contains("IoTDB down"));
        verify(mapper, atLeastOnce()).updateStatusAndContent(anyLong(), eq(2), eq("<html>hp2</html>"), isNull());
    }
}
