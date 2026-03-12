package com.zwei.module.iot.rule.handler.impl;

import com.zwei.module.iot.rule.handler.IotRuleActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 日志记录动作处理器
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Slf4j
@Component
public class LogActionHandler implements IotRuleActionHandler {

    @Override
    public String getSupportType() {
        return "log";
    }

    @Override
    public void execute(String actionParams, Map<String, Object> context) {
        log.info("Rule Action [LOG] triggered. Params: {}, Context: {}", actionParams, context);
    }
}
