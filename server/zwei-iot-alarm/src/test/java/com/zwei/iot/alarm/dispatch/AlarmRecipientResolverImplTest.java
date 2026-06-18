package com.zwei.iot.alarm.dispatch;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmRecipientQueryMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmRecipientResolverImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRecipientResolverImplTest {

    @Mock private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Mock private AlarmRecipientQueryMapper queryMapper;
    @InjectMocks private AlarmRecipientResolverImpl resolver;

    @Test
    void resolveUserIds_with_role_specific_ids() {
        when(recipientMapper.selectByRuleId(1L)).thenReturn(List.of(
            buildRecip("ROLE", "1"),
            buildRecip("ROLE", "2")
        ));
        when(queryMapper.selectUserIdsByRoleIds(Arrays.asList("1", "2")))
            .thenReturn(Arrays.asList(10L, 20L, 30L));

        Set<Long> result = resolver.resolveUserIds(1L);

        assertThat(result).containsExactlyInAnyOrder(10L, 20L, 30L);
    }

    @Test
    void resolveUserIds_with_role_wildcard_returns_all_users() {
        when(recipientMapper.selectByRuleId(2L)).thenReturn(List.of(
            buildRecip("ROLE", "*")
        ));
        when(queryMapper.selectAllActiveUserIds()).thenReturn(Arrays.asList(1L, 2L, 3L));

        Set<Long> result = resolver.resolveUserIds(2L);

        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void resolveUserIds_mixed_types_deduplicates() {
        when(recipientMapper.selectByRuleId(3L)).thenReturn(List.of(
            buildRecip("USER", "1"),
            buildRecip("USER", "2"),
            buildRecip("DEPT", "100")
        ));
        // dept 100 的用户含 id=1
        when(queryMapper.selectUserIdsByDeptIds(List.of("100")))
            .thenReturn(Arrays.asList(1L, 5L));

        Set<Long> result = resolver.resolveUserIds(3L);

        // 1 来自 USER 和 DEPT，应去重
        assertThat(result).containsExactlyInAnyOrder(1L, 2L, 5L);
    }

    private AlarmDispatchRuleRecipient buildRecip(String type, String id) {
        AlarmDispatchRuleRecipient r = new AlarmDispatchRuleRecipient();
        r.setRuleId(0L);
        r.setRecipientType(type);
        r.setRecipientId(id);
        return r;
    }
}
