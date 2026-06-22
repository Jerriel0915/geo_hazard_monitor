package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.ConditionGroup;
import com.zwei.iot.alarm.domain.LevelCondition;
import com.zwei.iot.alarm.domain.LevelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 判据条件评估器 V3.0 — 基于 level_config 的逐级多指标评估。
 *
 * @author zwei
 */
@Component
public class CriteriaEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CriteriaEvaluator.class);

    /**
     * 告警等级 key 顺序（从高到低）
     */
    private static final String[] LEVEL_KEYS = {"red", "orange", "yellow", "blue"};
    private static final Map<String, Integer> LEVEL_VALUES = Map.of(
            "red", 1, "orange", 2, "yellow", 3, "blue", 4
    );

    private static final java.util.Set<String> KINDS = java.util.Set.of("current", "prev");
    private static final java.util.Set<String> DIMENSIONS = java.util.Set.of("payload", "device", "packet");

    /**
     * 评估判据，返回触发的最高告警等级。
     *
     * @param criteria     判据
     * @param subjectValues 主语 → 当前值的映射（由调用方从 IoTDB 查询填充）
     * @return 告警等级 1-4，0 表示未触发
     */
    public int evaluate(AlarmCriteria criteria, Map<String, Double> subjectValues) {
        if (subjectValues == null || subjectValues.isEmpty()) {
            return 0;
        }

        Map<String, LevelConfig> configMap = parseLevelConfig(criteria.getLevelConfig());
        if (configMap.isEmpty()) {
            return 0;
        }

        // 从高到低检查
        for (String key : LEVEL_KEYS) {
            LevelConfig config = configMap.get(key);
            if (config == null) continue;

            boolean matched = evaluateLevel(config, subjectValues);
            if (matched) {
                return LEVEL_VALUES.getOrDefault(key, 0);
            }
        }
        return 0;
    }

    /**
     * 评估单个等级的所有条件。
     * <p>
     * 支持两种格式：
     * <ul>
     *   <li><b>groups 格式</b>（前端 GroupedRuleBuilder 生成）— 多条件组，组间 AND/OR</li>
     *   <li><b>conditions 格式</b>（旧/直接 SQL 创建）— 单层条件列表</li>
     * </ul>
     */
    boolean evaluateLevel(LevelConfig config, Map<String, Double> subjectValues) {
        if (config == null) return false;

        // 优先 groups 格式
        List<ConditionGroup> groups = config.getGroups();
        if (groups != null && !groups.isEmpty()) {
            boolean result = evaluateGroups(groups, config.getGroupLogic(), subjectValues);
            log.debug("[Alarm][Criteria][Level] groups 评估结果={} groupLogic={} groupCount={} subjects={}",
                    result, config.getGroupLogic(), groups.size(), subjectValues.keySet());
            return result;
        }

        // 旧 conditions 格式
        List<LevelCondition> conditions = config.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            log.debug("[Alarm][Criteria][Level] 等级配置无 groups 且无 conditions，跳过");
            return false;
        }

        boolean isOr = "OR".equalsIgnoreCase(config.getLogicOperator());
        for (LevelCondition cond : conditions) {
            Double value = resolveSubjectValue(cond, subjectValues);
            boolean condResult = evaluateCondition(cond, value);
            log.debug("[Alarm][Criteria][Level] condition result={} logic={} subject={} operator={} threshold={} actual={}",
                    condResult, isOr ? "OR" : "AND", cond.getSubject(), cond.getOperator(),
                    cond.getThreshold(), value);

            if (isOr && condResult) return true;       // OR: 任一满足即通过
            if (!isOr && !condResult) return false;     // AND: 任一不满足即失败
        }
        return !isOr; // AND: 全部通过; OR: 全部不通过
    }

    /**
     * 评估多个条件组，组间按 groupLogic (AND/OR) 组合。
     */
    private boolean evaluateGroups(List<ConditionGroup> groups, String groupLogic,
                                   Map<String, Double> subjectValues) {
        boolean isOr = "OR".equalsIgnoreCase(groupLogic);
        for (ConditionGroup group : groups) {
            boolean groupResult = evaluateSingleGroup(group, subjectValues);
            if (isOr && groupResult) return true;
            if (!isOr && !groupResult) return false;
        }
        return !isOr;
    }

    /**
     * 评估单个条件组（组内 conditions 按 logicOperator 组合）。
     */
    private boolean evaluateSingleGroup(ConditionGroup group, Map<String, Double> subjectValues) {
        if (group == null) return false;
        List<LevelCondition> conditions = group.getConditions();
        if (conditions == null || conditions.isEmpty()) return false;

        boolean isOr = "OR".equalsIgnoreCase(group.getLogicOperator());
        for (LevelCondition cond : conditions) {
            Double value = resolveSubjectValue(cond, subjectValues);
            boolean condResult = evaluateCondition(cond, value);
            log.debug("[Alarm][Criteria][Group] condition result={} logic={} subject={} operator={} threshold={} actual={}",
                    condResult, isOr ? "OR" : "AND", cond.getSubject(), cond.getOperator(),
                    cond.getThreshold(), value);

            if (isOr && condResult) return true;
            if (!isOr && !condResult) return false;
        }
        return !isOr;
    }

    /**
     * 根据条件的主语解析出实际值。
     * <p>
     * subject 格式由 {@link #normalizeSubject} 校验: 3 段 ({kind}.{dimension}.{attrCode})
     * 或 4 段 ({sensorCode}.{kind}.{dimension}.{attrCode}), 非法格式返回 null。
     */
    Double resolveSubjectValue(LevelCondition cond, Map<String, Double> subjectValues) {
        if (cond == null || cond.getSubject() == null) return null;

        String subject = normalizeSubject(cond.getSubject());

        Double value = subjectValues.get(subject);
        if (value == null) {
            log.debug("[Alarm][Criteria][Subject] 未找到对应值 raw={} normalized={} subjectType={} available={}",
                    cond.getSubject(), subject, cond.getSubjectType(), subjectValues.keySet());
        }
        return value;
    }

    /**
     * 标准化 subject — 按段数 + 枚举值校验新格式。
     *
     * <p>合法格式:
     * <ul>
     *   <li>4 段: {@code sensorCode.{kind}.{dimension}.{attrCode}}</li>
     *   <li>3 段: {@code {kind}.{dimension}.{attrCode}}</li>
     * </ul>
     * 老格式 {@code payload.current.x} / {@code payload.x} 不再兼容。
     */
    private String normalizeSubject(String subject) {
        if (subject == null) return null;
        String s = subject.trim();
        if (s.isEmpty()) return null;
        String[] parts = s.split("\\.");
        if (parts.length == 4) {
            if (!KINDS.contains(parts[1])) {
                log.warn("[Alarm][Criteria] 非法 kind: {} (subject={})", parts[1], subject);
                return null;
            }
            if (!DIMENSIONS.contains(parts[2])) {
                log.warn("[Alarm][Criteria] 非法 dimension: {} (subject={})", parts[2], subject);
                return null;
            }
            return s;
        } else if (parts.length == 3) {
            if (!KINDS.contains(parts[0])) {
                log.warn("[Alarm][Criteria] 非法 kind: {} (subject={})", parts[0], subject);
                return null;
            }
            if (!DIMENSIONS.contains(parts[1])) {
                log.warn("[Alarm][Criteria] 非法 dimension: {} (subject={})", parts[1], subject);
                return null;
            }
            return s;
        }
        log.warn("[Alarm][Criteria] 不支持的 subject 段数: {} (subject={})", parts.length, subject);
        return null;
    }

    /**
     * 评估单个条件。
     */
    boolean evaluateCondition(LevelCondition cond, Double value) {
        if (value == null || cond == null || cond.getOperator() == null) {
            log.debug("[Alarm][Criteria][Cond] 输入无效 value={} operator={} subject={}",
                    value, cond != null ? cond.getOperator() : null, cond != null ? cond.getSubject() : null);
            return false;
        }
        Double threshold = cond.getThreshold();
        if (threshold == null) {
            log.debug("[Alarm][Criteria][Cond] threshold=null 跳过 subject={} operator={}",
                    cond.getSubject(), cond.getOperator());
            return false;
        }

        switch (cond.getOperator().toUpperCase()) {
            case "GT":
                return value > threshold;
            case "GTE":
                return value >= threshold;
            case "LT":
                return value < threshold;
            case "LTE":
                return value <= threshold;
            case "EQ":
                return Math.abs(value - threshold) < 0.0001;
            case "NEQ":
                return Math.abs(value - threshold) >= 0.0001;
            case "BETWEEN":
                if (cond.getThresholdMax() == null) {
                    log.debug("[Alarm][Criteria][Cond] BETWEEN thresholdMax=null 跳过 subject={}", cond.getSubject());
                    return false;
                }
                return value >= threshold && value <= cond.getThresholdMax();
            default:
                log.debug("[Alarm][Criteria][Cond] 未知 operator 跳过 subject={} operator={}",
                        cond.getSubject(), cond.getOperator());
                return false;
        }
    }

    /**
     * 解析 level_config JSON。
     */
    public Map<String, LevelConfig> parseLevelConfig(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return JSON.parseObject(json, new TypeReference<LinkedHashMap<String, LevelConfig>>() {});
        } catch (Exception e) {
            log.warn("level_config 解析失败: {}", json, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 从 level_config 中提取所有引用的主语（用于缓存预判用到的指标列表）。
     */
    public List<String> extractSubjects(String levelConfigJson) {
        Map<String, LevelConfig> map = parseLevelConfig(levelConfigJson);
        return map.values().stream()
                .flatMap(c -> c.getConditions().stream())
                .map(LevelCondition::getSubject)
                .distinct()
                .toList();
    }
}
