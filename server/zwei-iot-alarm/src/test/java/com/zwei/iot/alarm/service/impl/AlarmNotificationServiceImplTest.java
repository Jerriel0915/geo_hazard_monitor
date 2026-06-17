package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.mapper.AlarmNotificationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AlarmNotificationServiceImpl 全链路单元测试 — 验证 Service 层在事件→落库→状态推进各环节
 * 的参数传递、状态码映射、空集合守卫与时间戳填充（不依赖真实 DB）。
 *
 * <p>Plan C Task 12 落地为 Mockito 单元测试（同 Plan B 16 / Plan C 6 的策略），原因：
 * {@code @SpringBootTest} 全量集成需要 sys_config / sys_user / alarm_dispatch_rule 真实数据，
 * 不适合 CI；端到端真实验证通过启动 local profile 手动触发告警事件查看 alarm_notification 表完成。</p>
 */
@ExtendWith(MockitoExtension.class)
class AlarmNotificationServiceImplTest {

    @Mock
    private AlarmNotificationMapper mapper;

    @InjectMocks
    private AlarmNotificationServiceImpl service;

    // ============= batchCreate =============

    @Test
    void batchCreate_emptyList_shortCircuits_noMapperCall() {
        int affected = service.batchCreate(Collections.emptyList());

        assertThat(affected).isZero();
        verify(mapper, never()).batchInsert(anyList());
    }

    @Test
    void batchCreate_nullList_shortCircuits_noMapperCall() {
        int affected = service.batchCreate(null);

        assertThat(affected).isZero();
        verify(mapper, never()).batchInsert(anyList());
    }

    @Test
    void batchCreate_validList_fillsCreateTime_andDelegatesToMapper() {
        AlarmNotification n1 = new AlarmNotification();
        n1.setRecipientId(1L);
        n1.setChannel("SYSTEM");
        AlarmNotification n2 = new AlarmNotification();
        n2.setRecipientId(2L);
        n2.setChannel("SMS");
        when(mapper.batchInsert(anyList())).thenReturn(2);

        int affected = service.batchCreate(List.of(n1, n2));

        assertThat(affected).isEqualTo(2);
        ArgumentCaptor<List<AlarmNotification>> captor =
            ArgumentCaptor.forClass(List.class);
        verify(mapper).batchInsert(captor.capture());
        for (AlarmNotification n : captor.getValue()) {
            assertThat(n.getCreateTime()).as("batchCreate 必须为每条通知填充 createTime").isNotNull();
        }
    }

    // ============= markFailed 状态码映射 =============

    @Test
    void markFailed_recipientPhoneMissing_mapsToStatusInvalidRecipient() {
        when(mapper.updateStatus(any(), eq(AlarmNotification.STATUS_INVALID_RECIPIENT),
            any(), any())).thenReturn(1);

        service.markFailed(100L, "RECIPIENT_PHONE_MISSING", "手机号为空");

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).updateStatus(eq(100L), eq(AlarmNotification.STATUS_INVALID_RECIPIENT),
            any(), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).contains("RECIPIENT_PHONE_MISSING");
        assertThat(msgCaptor.getValue()).contains("手机号为空");
    }

    @Test
    void markFailed_channelNotConfigured_mapsToStatusChannelNotConfigured() {
        when(mapper.updateStatus(any(), eq(AlarmNotification.STATUS_CHANNEL_NOT_CONFIGURED),
            any(), any())).thenReturn(1);

        service.markFailed(101L, "CHANNEL_NOT_CONFIGURED", "SMS 渠道未配置");

        verify(mapper).updateStatus(eq(101L), eq(AlarmNotification.STATUS_CHANNEL_NOT_CONFIGURED),
            any(), any());
    }

    @Test
    void markFailed_unknownErrorCode_mapsToStatusFailed() {
        when(mapper.updateStatus(any(), eq(AlarmNotification.STATUS_FAILED),
            any(), any())).thenReturn(1);

        service.markFailed(102L, "SMS_PROVIDER_TIMEOUT", "阿里云超时");

        verify(mapper).updateStatus(eq(102L), eq(AlarmNotification.STATUS_FAILED),
            any(), any());
    }

    @Test
    void markFailed_nullErrorCode_mapsToStatusFailed() {
        when(mapper.updateStatus(any(), eq(AlarmNotification.STATUS_FAILED),
            any(), any())).thenReturn(1);

        service.markFailed(103L, null, "未知错误");

        verify(mapper).updateStatus(eq(103L), eq(AlarmNotification.STATUS_FAILED),
            any(), any());
    }

    // ============= markSent =============

    @Test
    void markSent_alwaysUsesStatusSentConstant() {
        when(mapper.updateStatus(any(), eq(AlarmNotification.STATUS_SENT),
            any(), any())).thenReturn(1);

        service.markSent(200L);

        verify(mapper).updateStatus(eq(200L), eq(AlarmNotification.STATUS_SENT),
            any(), eq(null));
    }

    // ============= 用户视角接口透传 =============

    @Test
    void selectUserRecent_delegatesUntouched() {
        when(mapper.selectUserRecent(1L, 10)).thenReturn(Collections.emptyList());

        service.selectUserRecent(1L, 10);

        verify(mapper).selectUserRecent(1L, 10);
    }

    @Test
    void selectUnreadCount_delegatesWithChannel() {
        when(mapper.selectUnreadCount(7L, "SYSTEM")).thenReturn(3);

        int count = service.selectUnreadCount(7L, "SYSTEM");

        assertThat(count).isEqualTo(3);
        verify(mapper).selectUnreadCount(7L, "SYSTEM");
    }

    @Test
    void markReadIfOwner_delegatesUntouched() {
        when(mapper.markReadIfOwner(500L, 7L)).thenReturn(1);

        int affected = service.markReadIfOwner(500L, 7L);

        assertThat(affected).isEqualTo(1);
        verify(mapper).markReadIfOwner(500L, 7L);
    }

    @Test
    void markAllRead_delegatesWithUserAndChannel() {
        when(mapper.markAllRead(7L, "SYSTEM")).thenReturn(5);

        int affected = service.markAllRead(7L, "SYSTEM");

        assertThat(affected).isEqualTo(5);
        verify(mapper).markAllRead(7L, "SYSTEM");
    }
}
