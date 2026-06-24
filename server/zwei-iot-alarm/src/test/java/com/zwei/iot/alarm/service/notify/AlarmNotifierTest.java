package com.zwei.iot.alarm.service.notify;

import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.DeviceOfflineEvent;
import com.zwei.iot.alarm.channel.AlarmChannelDispatcher;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.service.IAlarmRecipientResolver;
import com.zwei.iot.alarm.dispatch.service.IAlarmRuleMatcher;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import com.zwei.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AlarmNotifier 单元测试 — 验证告警/离线事件的规则匹配、用户筛选、
 * userId×channel 去重与分发编排逻辑（不依赖真实 DB）。
 *
 * <p>Plan B Task 16 实际落地为 Mockito 单元测试（覆盖核心分发逻辑），
 * 而非 {@code @SpringBootTest} 全量集成测试 — 后者需要 alarm_dispatch_rule /
 * sys_user / sys_user_role 等真实数据准备，不适合纳入 CI。需要端到端验证时，
 * 启动 local profile 手工触发告警事件查看 alarm_notification 表即可。</p>
 */
@ExtendWith(MockitoExtension.class)
class AlarmNotifierTest {

    @Mock private IAlarmRuleMatcher ruleMatcher;
    @Mock private IAlarmRecipientResolver recipientResolver;
    @Mock private IAlarmNotificationService notificationService;
    @Mock private AlarmChannelDispatcher channelDispatcher;
    @Mock private ISysUserService userService;

    @InjectMocks
    private AlarmNotifier notifier;

    // ============= 告警事件 =============

    @Test
    void onAlarmTriggered_happy_path_builds_and_dispatches() {
        // given: 2 用户 × 2 渠道 → 4 条通知
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            900L, 1L, 4, "THRESHOLD", "测试告警", "首次告警");

        AlarmDispatchRule rule = buildRule(7L, "SYSTEM,SMS");
        when(ruleMatcher.matchAlarmRules(1L, "4", "THRESHOLD")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(7L))
            .thenReturn(new HashSet<>(Arrays.asList(10L, 20L)));
        when(userService.selectUserById(10L)).thenReturn(buildUser(10L, "0", "user10"));
        when(userService.selectUserById(20L)).thenReturn(buildUser(20L, "0", "user20"));
        when(notificationService.batchCreate(anyList())).thenReturn(4);

        // when
        notifier.onAlarmTriggered(event);

        // then — 落库一次, 分发 4 次
        verify(notificationService, times(1)).batchCreate(anyList());
        verify(channelDispatcher, times(4)).dispatch(any(AlarmNotification.class));
    }

    @Test
    void onAlarmTriggered_no_matching_rule_skips() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            901L, 2L, 3, "THRESHOLD", "无规则", "首次");

        when(ruleMatcher.matchAlarmRules(2L, "3", "THRESHOLD")).thenReturn(Collections.emptyList());

        notifier.onAlarmTriggered(event);

        verify(notificationService, never()).batchCreate(anyList());
        verify(channelDispatcher, never()).dispatch(any());
    }

    @Test
    void onAlarmTriggered_disabled_user_skipped() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            902L, 3L, 4, "THRESHOLD", "停用用户", "首次");

        AlarmDispatchRule rule = buildRule(8L, "SYSTEM");
        when(ruleMatcher.matchAlarmRules(3L, "4", "THRESHOLD")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(8L)).thenReturn(new HashSet<>(List.of(99L)));
        when(userService.selectUserById(99L))
            .thenReturn(buildUser(99L, "1", "disabled-user"));  // status="1" 停用

        notifier.onAlarmTriggered(event);

        verify(notificationService, never()).batchCreate(anyList());
        verify(channelDispatcher, never()).dispatch(any());
    }

    @Test
    void onAlarmTriggered_dedup_same_user_channel_in_multiple_rules() {
        // 同一用户在两条规则里都配置了 SYSTEM 渠道 → 只产生一条 SYSTEM + 一条 SMS
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            903L, 4L, 4, "THRESHOLD", "去重", "首次");

        AlarmDispatchRule ruleA = buildRule(11L, "SYSTEM");
        AlarmDispatchRule ruleB = buildRule(12L, "SYSTEM,SMS");
        when(ruleMatcher.matchAlarmRules(4L, "4", "THRESHOLD")).thenReturn(Arrays.asList(ruleA, ruleB));
        when(recipientResolver.resolveUserIds(11L)).thenReturn(new HashSet<>(List.of(50L)));
        when(recipientResolver.resolveUserIds(12L)).thenReturn(new HashSet<>(List.of(50L)));
        when(userService.selectUserById(50L)).thenReturn(buildUser(50L, "0", "user50"));
        when(notificationService.batchCreate(anyList())).thenReturn(2);

        notifier.onAlarmTriggered(event);

        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).batchCreate(captor.capture());
        List<AlarmNotification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(AlarmNotification::getChannel)
            .containsExactlyInAnyOrder("SYSTEM", "SMS");
        verify(channelDispatcher, times(2)).dispatch(any());
    }

    @Test
    void onAlarmTriggered_system_channel_defaults_to_sent_sms_pending() {
        // SYSTEM 渠道一定可达 → 创建时直接置为已发送；SMS/EMAIL 仍为待发送
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            905L, 6L, 4, "THRESHOLD", "状态默认", "首次");

        AlarmDispatchRule rule = buildRule(14L, "SYSTEM,SMS,EMAIL");
        when(ruleMatcher.matchAlarmRules(6L, "4", "THRESHOLD")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(14L)).thenReturn(new HashSet<>(List.of(70L)));
        when(userService.selectUserById(70L)).thenReturn(buildUser(70L, "0", "user70"));
        when(notificationService.batchCreate(anyList())).thenReturn(3);

        notifier.onAlarmTriggered(event);

        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).batchCreate(captor.capture());
        List<AlarmNotification> saved = captor.getValue();
        assertThat(saved).hasSize(3);

        AlarmNotification sysNotif = saved.stream()
            .filter(n -> "SYSTEM".equals(n.getChannel())).findFirst().orElseThrow();
        assertThat(sysNotif.getStatus()).isEqualTo(AlarmNotification.STATUS_SENT);

        AlarmNotification smsNotif = saved.stream()
            .filter(n -> "SMS".equals(n.getChannel())).findFirst().orElseThrow();
        assertThat(smsNotif.getStatus()).isEqualTo(AlarmNotification.STATUS_PENDING);

        AlarmNotification emailNotif = saved.stream()
            .filter(n -> "EMAIL".equals(n.getChannel())).findFirst().orElseThrow();
        assertThat(emailNotif.getStatus()).isEqualTo(AlarmNotification.STATUS_PENDING);

        // then — sourceType 派生正确
        assertThat(saved).allMatch(n -> "threshold".equals(n.getSourceType()));
        assertThat(saved.get(0).getTitle()).startsWith("[阈值告警]");
    }

    @Test
    void onAlarmTriggered_comprehensive_uses_correct_sourceType_and_title() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            906L, 7L, 4, "COMPREHENSIVE",
            "小时雨量80mm+土壤含水率85%，泥石流风险极高", "综合策略命中");

        AlarmDispatchRule rule = buildRule(15L, "SYSTEM");
        when(ruleMatcher.matchAlarmRules(7L, "4", "COMPREHENSIVE")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(15L)).thenReturn(new HashSet<>(List.of(80L)));
        when(userService.selectUserById(80L)).thenReturn(buildUser(80L, "0", "user80"));
        when(notificationService.batchCreate(anyList())).thenReturn(1);

        notifier.onAlarmTriggered(event);

        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).batchCreate(captor.capture());
        AlarmNotification saved = captor.getValue().get(0);
        assertThat(saved.getSourceType()).isEqualTo("comprehensive");
        assertThat(saved.getTitle()).startsWith("[综合告警]");
        assertThat(saved.getSourceId()).isEqualTo(906L);
    }

    // ============= 离线事件 =============

    @Test
    void onDeviceOffline_happy_path_dispatches() {
        DeviceOfflineEvent event = new DeviceOfflineEvent(
            700L, "client-700", "192.168.1.7", "keepalive timeout");

        AlarmDispatchRule rule = buildRule(9L, "SYSTEM");
        when(ruleMatcher.matchOfflineRules(700L)).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(9L)).thenReturn(new HashSet<>(List.of(30L)));
        when(userService.selectUserById(30L)).thenReturn(buildUser(30L, "0", "user30"));
        when(notificationService.batchCreate(anyList())).thenReturn(1);

        notifier.onDeviceOffline(event);

        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(notificationService, times(1)).batchCreate(captor.capture());
        AlarmNotification n = captor.getValue().get(0);
        assertThat(n.getSourceType()).isEqualTo("offline");
        assertThat(n.getSourceId()).isEqualTo(700L);
        assertThat(n.getAlarmId()).isEqualTo(700L);   // alarmId 兼容字段
        verify(channelDispatcher, times(1)).dispatch(any());
    }

    // ============= 异常路径 =============

    @Test
    void onAlarmTriggered_duplicate_key_silently_swallowed() {
        AlarmTriggeredEvent event = new AlarmTriggeredEvent(
            904L, 5L, 4, "THRESHOLD", "重放", "首次");

        AlarmDispatchRule rule = buildRule(13L, "SYSTEM");
        when(ruleMatcher.matchAlarmRules(5L, "4", "THRESHOLD")).thenReturn(List.of(rule));
        when(recipientResolver.resolveUserIds(13L)).thenReturn(new HashSet<>(List.of(60L)));
        when(userService.selectUserById(60L)).thenReturn(buildUser(60L, "0", "user60"));
        when(notificationService.batchCreate(anyList()))
            .thenThrow(new DuplicateKeyException("uk_notif_dedup 冲突"));

        notifier.onAlarmTriggered(event);

        // 落库冲突 → 直接 return，不分发
        verify(channelDispatcher, never()).dispatch(any());
    }

    // ============= helpers =============

    private SysUser buildUser(Long id, String status, String name) {
        SysUser u = new SysUser();
        u.setUserId(id);
        u.setUserName(name);
        u.setStatus(status);
        u.setPhonenumber("1380000000" + (id % 10));
        return u;
    }

    private AlarmDispatchRule buildRule(Long id, String channelsCsv) {
        AlarmDispatchRule r = new AlarmDispatchRule();
        r.setId(id);
        r.setChannels(channelsCsv);
        return r;
    }
}
