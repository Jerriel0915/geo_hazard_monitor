package com.zwei.iot.alarm;

import com.zwei.iot.alarm.mapper.AlarmNotificationMapper;
import com.zwei.iot.alarm.service.impl.AlarmNotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知中心用户隔离测试 — 验证 Service 层不绕过 Mapper 的 userId 过滤。
 *
 * <p>SQL 层的 user 隔离已由 Mapper 单测覆盖；本测试聚焦 Service 层不会无意中
 * 把 currentUserId 替换成传入的 notifId/其他用户 ID，确保调用链上参数不被污染。</p>
 */
@ExtendWith(MockitoExtension.class)
class AlarmNotificationSecurityTest {

    @Mock
    private AlarmNotificationMapper mapper;

    @InjectMocks
    private AlarmNotificationServiceImpl service;

    @Test
    void markReadIfOwner_passesUserIdAsOwnerFilter_notAsId() {
        // 用户 A 尝试标记 id=10（属于用户 B）已读 — 必须把 userId=1 + notifId=10 都传给 Mapper
        // Mapper 的 SQL WHERE id=? AND recipient_id=? AND read_time IS NULL 会拒绝
        when(mapper.markReadIfOwner(10L, 1L)).thenReturn(0);

        int affected = service.markReadIfOwner(10L, 1L);

        assertThat(affected).isZero();
        verify(mapper).markReadIfOwner(10L, 1L);
    }

    @Test
    void markReadIfOwner_nullInputs_doesNotTouchMapper() {
        int affected = service.markReadIfOwner(null, 1L);

        assertThat(affected).isZero();
        verify(mapper, never()).markReadIfOwner(anyLong(), anyLong());
    }

    @Test
    void selectUserRecent_passesUserIdUntouched() {
        when(mapper.selectUserRecent(1L, 50)).thenReturn(java.util.Collections.emptyList());

        service.selectUserRecent(1L, 50);

        // 验证 userId 不被篡改
        verify(mapper).selectUserRecent(eq(1L), anyInt());
    }

    @Test
    void markAllRead_onlyAffectsCallingUserChannel() {
        // markAllRead 必须把 (userId, channel) 都传给 Mapper，确保 SQL WHERE 命中正确范围
        when(mapper.markAllRead(1L, "SYSTEM")).thenReturn(3);

        int affected = service.markAllRead(1L, "SYSTEM");

        assertThat(affected).isEqualTo(3);
        verify(mapper).markAllRead(1L, "SYSTEM");
    }

    @Test
    void selectUnreadCount_passesUserIdAndChannel() {
        when(mapper.selectUnreadCount(2L, "SYSTEM")).thenReturn(5);

        int count = service.selectUnreadCount(2L, "SYSTEM");

        assertThat(count).isEqualTo(5);
        verify(mapper).selectUnreadCount(2L, "SYSTEM");
    }
}
