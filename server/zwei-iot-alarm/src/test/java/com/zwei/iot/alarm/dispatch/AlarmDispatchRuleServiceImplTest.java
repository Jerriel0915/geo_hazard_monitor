package com.zwei.iot.alarm.dispatch;

import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.LoginUser;
import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleDeviceMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleHazardPointMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmDispatchRuleServiceImpl;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmDispatchRuleServiceImplTest {

    @Mock private AlarmDispatchRuleMapper ruleMapper;
    @Mock private AlarmDispatchRuleHazardPointMapper hpMapper;
    @Mock private AlarmDispatchRuleDeviceMapper deviceMapper;
    @Mock private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Mock private ISysRoleService roleService;
    @Mock private ISysDeptService deptService;
    @Mock private ISysUserService userService;

    @InjectMocks
    private AlarmDispatchRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        LoginUser loginUser = new LoginUser();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("testuser");
        loginUser.setUser(sysUser);
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AlarmDispatchRuleCreateRequest buildAlarmReq() {
        AlarmDispatchRuleCreateRequest req = new AlarmDispatchRuleCreateRequest();
        req.setName("测试规则");
        req.setEventType("ALARM");
        req.setAlarmLevels(Arrays.asList("3", "4"));
        req.setChannels(Arrays.asList("SYSTEM", "SMS"));
        req.setHazardPointIds(Arrays.asList("1", "2"));
        req.setIsEnabled(1);

        AlarmDispatchRuleCreateRequest.RecipientSelection rs =
            new AlarmDispatchRuleCreateRequest.RecipientSelection();
        rs.setRoleIds(Arrays.asList("1"));
        rs.setUserIds(Arrays.asList("1", "2"));
        req.setRecipients(rs);
        return req;
    }

    @Test
    void create_alarm_should_insert_main_and_three_relation_tables() {
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();
        int result = service.create(req);
        assertThat(result).isEqualTo(1);
        verify(ruleMapper, times(1)).insert(any());
        verify(hpMapper, times(1)).batchInsert(anyList());
        verify(recipientMapper, times(1)).batchInsert(anyList());
        verify(deviceMapper, never()).batchInsert(anyList());
    }

    @Test
    void create_offline_should_insert_device_relation_not_hazard() {
        AlarmDispatchRuleCreateRequest req = new AlarmDispatchRuleCreateRequest();
        req.setName("离线规则");
        req.setEventType("OFFLINE");
        req.setChannels(Arrays.asList("SYSTEM"));
        req.setDeviceIds(Arrays.asList("10", "11"));
        req.setIsEnabled(1);
        AlarmDispatchRuleCreateRequest.RecipientSelection rs =
            new AlarmDispatchRuleCreateRequest.RecipientSelection();
        rs.setUserIds(Arrays.asList("*"));
        req.setRecipients(rs);

        service.create(req);

        verify(ruleMapper, times(1)).insert(any());
        verify(deviceMapper, times(1)).batchInsert(anyList());
        verify(hpMapper, never()).batchInsert(anyList());
    }

    @Test
    void update_should_delete_then_insert_relations() {
        Long id = 100L;
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();
        when(ruleMapper.selectById(id)).thenReturn(new com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule());

        service.update(id, req);

        verify(hpMapper, times(1)).deleteByRuleId(id);
        verify(hpMapper, times(1)).batchInsert(anyList());
        verify(recipientMapper, times(1)).deleteByRuleId(id);
        verify(recipientMapper, times(1)).batchInsert(anyList());
    }

    @Test
    void delete_should_logic_delete_main_and_physical_delete_relations() {
        Long id = 100L;
        service.delete(id);
        verify(ruleMapper, times(1)).logicDeleteById(id);
        verify(hpMapper, times(1)).deleteByRuleId(id);
        verify(deviceMapper, times(1)).deleteByRuleId(id);
        verify(recipientMapper, times(1)).deleteByRuleId(id);
    }

    @Test
    void toggleEnabled_should_call_updateEnabled() {
        Long id = 100L;
        service.toggleEnabled(id, 0);
        verify(ruleMapper, times(1)).updateEnabled(id, 0);
    }

    @Test
    void wildcard_normalization_when_id_is_wildcard_plus_specific() {
        AlarmDispatchRuleCreateRequest req = buildAlarmReq();
        // 隔离测试：清空 dept/user，只测 role 通配符归一化
        req.getRecipients().setRoleIds(Arrays.asList("*", "1", "2"));
        req.getRecipients().setUserIds(null);
        req.getRecipients().setDeptIds(null);

        service.create(req);

        verify(recipientMapper, times(1)).batchInsert(argThat(list ->
            list.size() == 1
            && "ROLE".equals(((AlarmDispatchRuleRecipient) list.get(0)).getRecipientType())
            && "*".equals(((AlarmDispatchRuleRecipient) list.get(0)).getRecipientId())
        ));
    }
}
