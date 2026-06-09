package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.zwei.iot.alarm.domain.AlarmCriteria;
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
            "red", 4, "orange", 3, "yellow", 2, "blue", 1
    );

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
     */
    boolean evaluateLevel(LevelConfig config, Map<String, Double> subjectValues) {
        List<LevelCondition> conditions = config.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        boolean isOr = "OR".equalsIgnoreCase(config.getLogicOperator());
        for (LevelCondition cond : conditions) {
            Double value = resolveSubjectValue(cond, subjectValues);
            boolean condResult = evaluateCondition(cond, value);

            if (isOr && condResult) return true;       // OR: 任一满足即通过
            if (!isOr && !condResult) return false;     // AND: 任一不满足即失败
        }
        return !isOr; // AND: 全部通过; OR: 全部不通过
    }

    /**
     * 根据条件的主语解析出实际值。
     */
    Double resolveSubjectValue(LevelCondition cond, Map<String, Double> subjectValues) {
        if (cond == null || cond.getSubject() == null) return null;

        if ("FUNCTION".equals(cond.getSubjectType())) {
            // 函数主语：尝试从 subjectValues 中查找（由调用方预计算函数值）
            return subjectValues.get(cond.getSubject());
        }
        // 直接监测内容
        return subjectValues.get(cond.getSubject());
    }

    /**
     * 评估单个条件。
     */
    boolean evaluateCondition(LevelCondition cond, Double value) {
        if (value == null || cond == null || cond.getOperator() == null) {
            return false;
        }
        Double threshold = cond.getThreshold();
        if (threshold == null) return false;

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
                return cond.getThresholdMax() != null
                        && value >= threshold && value <= cond.getThresholdMax();
            default:
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
