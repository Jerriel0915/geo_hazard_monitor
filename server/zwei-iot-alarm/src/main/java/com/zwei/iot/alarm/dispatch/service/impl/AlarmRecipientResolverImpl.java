package com.zwei.iot.alarm.dispatch.service.impl;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRuleRecipient;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleRecipientMapper;
import com.zwei.iot.alarm.dispatch.mapper.AlarmRecipientQueryMapper;
import com.zwei.iot.alarm.dispatch.service.IAlarmRecipientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlarmRecipientResolverImpl implements IAlarmRecipientResolver {

    private static final String WILDCARD = "*";

    @Autowired private AlarmDispatchRuleRecipientMapper recipientMapper;
    @Autowired private AlarmRecipientQueryMapper queryMapper;

    @Override
    public Set<Long> resolveUserIds(Long ruleId) {
        List<AlarmDispatchRuleRecipient> recips = recipientMapper.selectByRuleId(ruleId);
        if (recips == null || recips.isEmpty()) return Collections.emptySet();

        // 按类型分组
        Map<String, List<String>> byType = recips.stream()
            .collect(Collectors.groupingBy(
                AlarmDispatchRuleRecipient::getRecipientType,
                Collectors.mapping(
                    AlarmDispatchRuleRecipient::getRecipientId,
                    Collectors.toList())));

        Set<Long> userIds = new HashSet<>();

        // ROLE
        List<String> roleIds = byType.getOrDefault("ROLE", Collections.emptyList());
        if (roleIds.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else if (!roleIds.isEmpty()) {
            userIds.addAll(queryMapper.selectUserIdsByRoleIds(roleIds));
        }

        // DEPT
        List<String> deptIds = byType.getOrDefault("DEPT", Collections.emptyList());
        if (deptIds.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else if (!deptIds.isEmpty()) {
            userIds.addAll(queryMapper.selectUserIdsByDeptIds(deptIds));
        }

        // USER
        List<String> userIdsStr = byType.getOrDefault("USER", Collections.emptyList());
        if (userIdsStr.contains(WILDCARD)) {
            userIds.addAll(queryMapper.selectAllActiveUserIds());
        } else {
            userIdsStr.stream()
                .filter(Objects::nonNull)
                .map(s -> {
                    try { return Long.parseLong(s); }
                    catch (NumberFormatException e) { return null; }
                })
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        }

        return userIds;
    }
}
