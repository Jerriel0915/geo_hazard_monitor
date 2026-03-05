package com.zwei.module.iot.rule.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * IoT规则动作对象 zw_iot_rule_action
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Data
public class IotRuleAction implements Serializable {
    private static final long serialVersionUID = 4533987455325582736L;

    /**
     * 动作ID
     */
    private Long actionId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 动作类型
     */
    private String actionType;

    /**
     * 动作参数(JSON)
     */
    private String actionParams;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
