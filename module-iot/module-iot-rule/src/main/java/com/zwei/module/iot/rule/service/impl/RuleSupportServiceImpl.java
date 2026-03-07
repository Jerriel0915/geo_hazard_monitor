package com.zwei.module.iot.rule.service.impl;

import com.googlecode.aviator.AviatorEvaluator;
import com.zwei.common.exception.ServiceException;
import com.zwei.module.iot.rule.service.IRuleSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 规则引擎测试服务
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-05
 */
@Slf4j
@Service
public class RuleSupportServiceImpl implements IRuleSupportService {

    @Override
    public boolean validate(String ruleExpression) {
        try {
            AviatorEvaluator.validate(ruleExpression);
            return true;
        } catch (Exception e) {
            log.warn("Rule expression validate failed: {}", e.getMessage());
            throw new ServiceException("规则表达式不合法: " + e.getMessage());
        }
    }

    @Override
    public boolean test(String ruleExpression, Map<String, Object> context) {
        try {
            Object res = AviatorEvaluator.execute(ruleExpression, context);
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            log.warn("Rule expression execute failed: {}", e.getMessage());
            throw new ServiceException("规则测试失败: " + e.getMessage());
        }
    }
}

