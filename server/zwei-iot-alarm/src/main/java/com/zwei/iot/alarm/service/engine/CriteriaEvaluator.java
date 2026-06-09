package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.CriteriaCondition;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 判据条件评估器 — 解析 alarm_criteria.conditions_json 并执行条件匹配。
 * <p>
 * 支持操作符: GT, GTE, LT, LTE, EQ, NEQ, BETWEEN
 *
 * @author zwei
 */
@Component
public class CriteriaEvaluator {

    /**
     * 评估单条判据是否触发告警。
     *
     * @param criteria     判据
     * @param currentValue 当前测量值
     * @param recentValues 最近N条历史值（用于 RATE_CHANGE 等复合操作符，暂无）
     * @return 触发的告警等级 (1=蓝 2=黄 3=橙 4=红)，0 = 未触发
     */
    public int evaluate(AlarmCriteria criteria, Double currentValue) {
        String conditionsJson = criteria.getConditionsJson();
        if (conditionsJson == null || conditionsJson.isEmpty()) {
            // 无条件 JSON，仅按表达式阈值判定
            return evaluateLevelExpressions(criteria, currentValue);
        }

        List<CriteriaCondition> conditions = parseConditions(conditionsJson);
        if (conditions.isEmpty()) {
            return evaluateLevelExpressions(criteria, currentValue);
        }

        // 评估条件组合
        boolean matched;
        if ("OR".equalsIgnoreCase(criteria.getLogicOperator())) {
            matched = conditions.stream().anyMatch(c -> evaluateSingleCondition(c, currentValue));
        } else {
            // 默认 AND
            matched = conditions.stream().allMatch(c -> evaluateSingleCondition(c, currentValue));
        }

        if (!matched) {
            return 0;
        }

        // 分级判定：从高到低依次检查
        return evaluateLevelExpressions(criteria, currentValue);
    }

    /**
     * 评估单个条件是否满足。
     */
    private boolean evaluateSingleCondition(CriteriaCondition cond, Double currentValue) {
        if (currentValue == null) {
            return false;
        }
        String operator = cond.getOperator();
        Double threshold = cond.getThreshold();

        switch (operator != null ? operator.toUpperCase() : "GT") {
            case "GT":
                return currentValue > threshold;
            case "GTE":
                return currentValue >= threshold;
            case "LT":
                return currentValue < threshold;
            case "LTE":
                return currentValue <= threshold;
            case "EQ":
                return Math.abs(currentValue - threshold) < 0.0001;
            case "NEQ":
                return Math.abs(currentValue - threshold) >= 0.0001;
            case "BETWEEN":
                Double max = cond.getThresholdMax();
                return max != null && currentValue >= threshold && currentValue <= max;
            default:
                return false;
        }
    }

    /**
     * 分级判定：从高到低检查各等级表达式阈值。
     */
    private int evaluateLevelExpressions(AlarmCriteria criteria, Double currentValue) {
        if (currentValue == null) return 0;
        if (matchesLevel(criteria.getRedExpression(), currentValue)) return 4;
        if (matchesLevel(criteria.getOrangeExpression(), currentValue)) return 3;
        if (matchesLevel(criteria.getYellowExpression(), currentValue)) return 2;
        if (matchesLevel(criteria.getBlueExpression(), currentValue)) return 1;
        return 0;
    }

    /**
     * 检查当前值是否超过指定等级的阈值表达式。
     * 表达式格式: 纯数字（如 "10.5"）表示 > 阈值；也可为 "LT 5" 表示低于阈值时触发。
     */
    private boolean matchesLevel(String expression, Double currentValue) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        try {
            String trimmed = expression.trim();
            if (trimmed.startsWith("LT ")) {
                double threshold = Double.parseDouble(trimmed.substring(3).trim());
                return currentValue < threshold;
            }
            if (trimmed.startsWith("GT ")) {
                double threshold = Double.parseDouble(trimmed.substring(3).trim());
                return currentValue > threshold;
            }
            if (trimmed.startsWith("LTE ")) {
                double threshold = Double.parseDouble(trimmed.substring(4).trim());
                return currentValue <= threshold;
            }
            if (trimmed.startsWith("GTE ")) {
                double threshold = Double.parseDouble(trimmed.substring(4).trim());
                return currentValue >= threshold;
            }
            // 默认: 纯数字 = GT 阈值
            double threshold = Double.parseDouble(trimmed);
            return currentValue > threshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析 conditions_json 为条件对象列表。
     */
    List<CriteriaCondition> parseConditions(String conditionsJson) {
        try {
            return JSON.parseArray(conditionsJson, CriteriaCondition.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
