package com.zwei.module.iot.rule.handler.impl;

import com.zwei.module.iot.rule.handler.IotRuleActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 告警动作处理器
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Slf4j
@Component
public class AlertActionHandler implements IotRuleActionHandler {

    @Override
    public String getSupportType() {
        return "alert";
    }

    @Override
    public void execute(String actionParams, Map<String, Object> context) {
        // TODO: 集成实际的告警服务（短信/邮件等）
        log.warn("Rule Action [ALERT] triggered! Params: {}, Context: {}", actionParams, context);
    }
}
