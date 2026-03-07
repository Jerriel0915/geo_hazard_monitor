package com.zwei.module.iot.rule.handler.impl;

import com.zwei.module.iot.rule.handler.IotRuleActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 服务调用动作处理器
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Slf4j
@Component
public class ServiceInvokeActionHandler implements IotRuleActionHandler {

    @Override
    public String getSupportType() {
        return "service";
    }

    @Override
    public void execute(String actionParams, Map<String, Object> context) {
        // TODO: 解析 params 调用具体的设备服务或业务服务
        log.info("Rule Action [SERVICE] triggered. Params: {}, Context: {}", actionParams, context);
    }
}
