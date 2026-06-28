package com.zwei.iot.alarm.service.engine;

import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 策略范围解析器，将 alarm_strategy_hazard_point 中的 scope 值
 * (*、group:{id}、{数字}) 展开为实际隐患点 ID 列表。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StrategyScopeResolver {

    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final IHazardPointQueryService hazardPointQueryService;

    /**
     * 展开策略 scope 为实际隐患点 ID 列表。
     */
    public List<Long> resolveScope(Long strategyId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        Set<Long> result = new LinkedHashSet<>();

        for (String scope : scopeValues) {
            try {
                if ("*".equals(scope)) {
                    List<HazardPointBrief> all = hazardPointQueryService.listMonitoring();
                    for (HazardPointBrief b : all) {
                        result.add(b.id());
                    }
                } else if (scope.startsWith("group:")) {
                    Long groupId = Long.parseLong(scope.substring(6));
                    result.addAll(hazardPointQueryService.listIdsByGroupId(groupId));
                } else {
                    result.add(Long.parseLong(scope));
                }
            } catch (Exception e) {
                log.warn("scope 值解析失败，跳过: strategyId={} scope={}", strategyId, scope, e);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 反向匹配：判断给定隐患点 ID 是否在策略 scope 内。
     * 用于事件监听器快速过滤。
     */
    public boolean isHazardPointInScope(Long strategyId, Long hazardPointId) {
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(strategyId);
        for (String scope : scopeValues) {
            if ("*".equals(scope)) return true;
            if (scope.startsWith("group:")) {
                try {
                    Long groupId = Long.parseLong(scope.substring(6));
                    if (hazardPointQueryService.listIdsByGroupId(groupId).contains(hazardPointId))
                        return true;
                } catch (NumberFormatException e) {
                    log.warn("group scope 解析失败: {}", scope);
                }
            } else if (scope.equals(String.valueOf(hazardPointId))) {
                return true;
            }
        }
        return false;
    }
}
