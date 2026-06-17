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
     * 多条件逻辑: AND / OR（旧格式 conditions 直接挂在 level 下时使用）
     */
    private String logicOperator = "AND";

    /**
     * 条件列表（旧格式 — 直接挂在 level 下）
     */
    private List<LevelCondition> conditions = Collections.emptyList();

    /**
     * 该等级的告警描述文本
     */
    private String description;

    // ── 前端 GroupedRuleBuilder 格式 ──

    /**
     * 条件组列表（前端生成格式，与 conditions 二选一）。
     * <p>非空时优先使用 groups 评估，忽略 conditions。
     */
    private List<ConditionGroup> groups;

    /**
     * 组间逻辑: AND / OR（groups 非空时生效）
     */
    private String groupLogic = "AND";

    /**
     * 该等级独立的持续触发次数（覆盖 criterion 级别的 persist_count）。
     */
    private Integer persistCount;

    /**
     * 该等级独立的静默周期（覆盖 criterion 级别的 silence_period）。
     */
    private Integer silencePeriod;

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

    public List<ConditionGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<ConditionGroup> groups) {
        this.groups = groups;
    }

    public String getGroupLogic() {
        return groupLogic;
    }

    public void setGroupLogic(String groupLogic) {
        this.groupLogic = groupLogic;
    }

    public Integer getPersistCount() {
        return persistCount;
    }

    public void setPersistCount(Integer persistCount) {
        this.persistCount = persistCount;
    }

    public Integer getSilencePeriod() {
        return silencePeriod;
    }

    public void setSilencePeriod(Integer silencePeriod) {
        this.silencePeriod = silencePeriod;
    }
}
