package com.zwei.iot.alarm.domain;

import java.util.Collections;
import java.util.List;

/**
 * 条件组 — level_config 中每个等级下的一个条件分组。
 *
 * <p>对应前端 GroupedRuleBuilder 生成的嵌套结构：
 * {@code level_config.{level}.groups[].conditions[]}
 *
 * @author zwei
 */
public class ConditionGroup {

    /**
     * 组内条件列表
     */
    private List<LevelCondition> conditions = Collections.emptyList();

    /**
     * 组内逻辑: AND / OR
     */
    private String logicOperator = "AND";

    // ── Getters / Setters ──
    public List<LevelCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<LevelCondition> conditions) {
        this.conditions = conditions;
    }

    public String getLogicOperator() {
        return logicOperator;
    }

    public void setLogicOperator(String logicOperator) {
        this.logicOperator = logicOperator;
    }
}
