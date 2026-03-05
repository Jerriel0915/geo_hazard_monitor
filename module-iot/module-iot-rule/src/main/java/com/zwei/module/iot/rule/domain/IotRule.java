package com.zwei.module.iot.rule.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * IoT规则对象 zw_iot_rule
 * 使用 Aviator 进行规则匹配
 * <br>关于 Aviator 的使用参考 <a href="https://github.com/googlemeoften/aviator/blob/master/README.md">...</a>
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IotRule extends BaseEntity {
    private static final long serialVersionUID = 8568461847656729825L;

    /**
     * 状态：停用
     */
    public static final String STATUS_DISABLE = "0";
    /**
     * 状态：启用
     */
    public static final String STATUS_ENABLE = "1";
    /**
     * 状态：草稿
     */
    public static final String STATUS_DRAFT = "2";

    /**
     * 规则ID，表主键
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 产品标识
     */
    private String productKey;

    /**
     * 设备标识(可选)
     */
    private String deviceKey;

    /**
     * 触发类型(property:属性上报, event:事件上报)
     */
    private String triggerType;

    /**
     * Aviator 规则表达式
     */
    private String ruleExpression;

    /**
     * 状态（0停用 1启用）
     */
    private String status;

    /**
     * 优先级, int型整数, 最低 0(默认等级), 不建议区分过多等级, 目前分3级(最高为 2 级), 数值越大匹配时优先度越高
     */
    private Integer priority;

    /**
     * 规则动作列表，关联表 zw_iot_rule_action
     */
    private List<IotRuleAction> actionList;
}
