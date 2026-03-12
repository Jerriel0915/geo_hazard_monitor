package com.zwei.module.iot.rule.handler;

import java.util.Map;

/**
 * 规则引擎动作处理器接口
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
public interface IotRuleActionHandler {

    /**
     * 获取支持的动作类型，必须确保返回的名字唯一(如 alert)
     *
     * @return 动作类型
     */
    String getSupportType();

    /**
     * 执行动作
     *
     * @param actionParams 动作参数
     * @param context      上下文数据
     */
    void execute(String actionParams, Map<String, Object> context);
}
