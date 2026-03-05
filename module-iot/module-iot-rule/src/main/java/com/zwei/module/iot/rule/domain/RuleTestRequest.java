package com.zwei.module.iot.rule.domain;

import lombok.Data;

import java.util.Map;

/**
 * 测试请求类
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-04
 */
@Data
public class RuleTestRequest {
    // 规则表达式
    private String ruleExpression;
    // 参数
    private Map<String, Object> context;
}

