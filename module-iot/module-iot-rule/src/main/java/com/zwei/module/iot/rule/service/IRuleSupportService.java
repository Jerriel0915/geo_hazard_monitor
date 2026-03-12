package com.zwei.module.iot.rule.service;

/**
 * 规则引擎测试服务接口
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-04
 */
public interface IRuleSupportService {
    // 语法校验
    boolean validate(String ruleExpression);

    // 规则测试（需要注入参数）
    boolean test(String ruleExpression, java.util.Map<String, Object> context);
}

