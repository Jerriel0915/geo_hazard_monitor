package com.zwei.iot.alarm.channel;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NotifyTemplateService 单元测试 — 验证 threshold/comprehensive sourceType
 * 能正确命中 AlarmRecord 查询分支与告警格式渲染。
 *
 * <p>告警类型拆分后 (ALARM → THRESHOLD/COMPREHENSIVE)，sourceType 由 "alarm"
 * 拆为 "threshold" / "comprehensive"。本测试确保模板服务对三种 sourceType
 * ("alarm" 兼容遗留 / "threshold" / "comprehensive") 均按告警路径处理，
 * 不误走 offline 分支。</p>
 */
@ExtendWith(MockitoExtension.class)
class NotifyTemplateServiceTest {

    @Mock private IAlarmRecordService alarmRecordService;

    @InjectMocks
    private NotifyTemplateService service;

    private AlarmRecord buildRecord() {
        return AlarmRecord.builder()
            .id(500L)
            .hazardPointName("龙泉寺崩塌")
            .deviceName("DEV-001")
            .alarmLevelText("橙色")
            .firstTriggerTime(new Date(1700000000000L))
            .build();
    }

    private AlarmNotification buildNotification(String sourceType) {
        return AlarmNotification.builder()
            .id(1L)
            .sourceId(500L)
            .sourceType(sourceType)
            .title("[阈值告警] 测试")
            .build();
    }

    // ============= buildContext: 告警路径触发 AlarmRecord 查询 =============

    @Test
    void buildContext_threshold_sourceType_looks_up_alarm_record() {
        AlarmNotification n = buildNotification("threshold");
        when(alarmRecordService.selectById(500L)).thenReturn(buildRecord());

        NotifyContext ctx = service.buildContext(n);

        verify(alarmRecordService).selectById(500L);
        assertThat(ctx.getHazardPointName()).isEqualTo("龙泉寺崩塌");
        assertThat(ctx.getDeviceName()).isEqualTo("DEV-001");
        assertThat(ctx.getAlarmLevel()).isEqualTo("橙色");
        assertThat(ctx.getEventTime()).isEqualTo(new Date(1700000000000L));
    }

    @Test
    void buildContext_comprehensive_sourceType_looks_up_alarm_record() {
        AlarmNotification n = buildNotification("comprehensive");
        when(alarmRecordService.selectById(500L)).thenReturn(buildRecord());

        NotifyContext ctx = service.buildContext(n);

        verify(alarmRecordService).selectById(500L);
        assertThat(ctx.getHazardPointName()).isEqualTo("龙泉寺崩塌");
        assertThat(ctx.getAlarmLevel()).isEqualTo("橙色");
    }

    @Test
    void buildContext_offline_sourceType_does_not_lookup_alarm_record() {
        AlarmNotification n = buildNotification("offline");

        service.buildContext(n);

        verify(alarmRecordService, org.mockito.Mockito.never()).selectById(org.mockito.ArgumentMatchers.any());
    }

    // ============= renderEmailSubject: 告警路径不走 offline 文案 =============

    @Test
    void renderEmailSubject_threshold_uses_alarm_format() {
        AlarmNotification n = buildNotification("threshold");
        when(alarmRecordService.selectById(500L)).thenReturn(buildRecord());

        String subject = service.renderEmailSubject(n);

        assertThat(subject).contains("[知微告警]");
        assertThat(subject).doesNotContain("设备离线");
    }

    @Test
    void renderEmailSubject_comprehensive_uses_alarm_format() {
        AlarmNotification n = buildNotification("comprehensive");
        when(alarmRecordService.selectById(500L)).thenReturn(buildRecord());

        String subject = service.renderEmailSubject(n);

        assertThat(subject).contains("[知微告警]");
        assertThat(subject).doesNotContain("设备离线");
    }

    @Test
    void renderEmailSubject_offline_uses_offline_format() {
        AlarmNotification n = buildNotification("offline");

        String subject = service.renderEmailSubject(n);

        assertThat(subject).contains("设备离线");
    }
}
