package com.zwei.iot.alarm.domain;

import java.util.Collections;
import java.util.List;

/**
 * 单个告警等级的判据条件配置。
 * <p>
 * 对应 alarm_criteria.level_config JSON 中的一个等级节点。
 *
 * @author zwei
 */
public class LevelConfig {

    /**
     * 多条件逻辑: AND / OR
     */
    private String logicOperator = "AND";

    /**
     * 条件列表
     */
    private List<LevelCondition> conditions = Collections.emptyList();

    /**
     * 该等级的告警描述文本
     */
    private String description;

    // ── Getters / Setters ──
    public String getLogicOperator() {
        return logicOperator;
    }

    public void setLogicOperator(String logicOperator) {
        this.logicOperator = logicOperator;
    }

    public List<LevelCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<LevelCondition> conditions) {
        this.conditions = conditions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
