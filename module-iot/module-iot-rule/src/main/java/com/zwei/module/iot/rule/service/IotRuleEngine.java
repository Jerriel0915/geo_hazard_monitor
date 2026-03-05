package com.zwei.module.iot.rule.service;

import com.googlecode.aviator.AviatorEvaluator;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.utils.StringUtils;
import com.zwei.module.iot.rule.domain.IotRule;
import com.zwei.module.iot.rule.domain.IotRuleAction;
import com.zwei.module.iot.rule.handler.IotRuleActionHandler;
import com.zwei.module.iot.rule.mapper.IotRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * IoT规则引擎核心服务
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-25
 */
@Slf4j
@Service
public class IotRuleEngine {
    private final IotRuleMapper ruleMapper;
    private final RedisCache redisCache;
    private final List<IotRuleActionHandler> actionHandlers;

    // 动作执行器(处理器)
    private final Map<String, IotRuleActionHandler> handlerMap = new ConcurrentHashMap<>();
    private static final String RULE_CACHE_PREFIX = "iot:rule:product:";

    @Autowired
    IotRuleEngine(IotRuleMapper ruleMapper, RedisCache redisCache, List<IotRuleActionHandler> actionHandlers) {
        this.ruleMapper = ruleMapper;
        this.redisCache = redisCache;
        this.actionHandlers = actionHandlers;
    }

    /**
     * 注册规则处理器，加载进内存
     */
    @PostConstruct
    public void init() {
        for (IotRuleActionHandler handler : actionHandlers) {
            handlerMap.put(handler.getSupportType(), handler);
            log.info("Registered Rule Action Handler: {}", handler.getSupportType());
        }
    }

    /**
     * 异步执行规则匹配，防止堵塞消息监听
     *
     * @param productKey 产品Key
     * @param deviceKey  设备Key
     * @param data       数据上下文 (属性名 -> 属性值)
     */
    @Async
    public void match(String productKey, String deviceKey, Map<String, Object> data) {
        try {
            // 获取该产品下所有启用的规则
            String cacheKey = RULE_CACHE_PREFIX + productKey;
            List<IotRule> rules = redisCache.getCacheList(cacheKey);
            if (rules == null || rules.isEmpty()) {
                rules = ruleMapper.selectActiveRulesByProductKey(productKey);
                if (rules != null && !rules.isEmpty()) {
                    redisCache.setCacheList(cacheKey, rules);
                    redisCache.expire(cacheKey, 1, TimeUnit.HOURS);
                }
            }

            if (rules == null || rules.isEmpty()) {
                return;
            }

            log.debug("Start matching rules for device: {}, data: {}", deviceKey, data);

            for (IotRule rule : rules) {
                // 由于先前只以 productKey 进行筛选，因此在这需要过滤特定设备的规则
                if (StringUtils.isNotEmpty(rule.getDeviceKey()) && !rule.getDeviceKey().equals(deviceKey)) {
                    continue;
                }

                // 执行表达式匹配
                try {
                    // Aviator执行表达式，返回布尔值
                    Object result = AviatorEvaluator.execute(rule.getRuleExpression(), data);

                    if (result instanceof Boolean && (Boolean) result) {
                        log.info("Rule matched! ruleId={}, name={}, expression={}",
                                rule.getRuleId(), rule.getRuleName(), rule.getRuleExpression());

                        // 触发动作
                        triggerActions(rule.getActionList(), data);
                    }
                } catch (Exception e) {
                    log.error("Rule evaluation error. ruleId={}, expression={}, data={}",
                            rule.getRuleId(), rule.getRuleExpression(), data, e);
                }
            }
        } catch (Exception e) {
            log.error("Rule engine error", e);
        }
    }

    /**
     * 触发器
     *
     * @param actions 动作列表
     * @param context 规则触发时的数据上下文
     */
    private void triggerActions(List<IotRuleAction> actions, Map<String, Object> context) {
        if (actions == null || actions.isEmpty()) {
            return;
        }

        for (IotRuleAction action : actions) {
            try {
                String type = action.getActionType();
                IotRuleActionHandler handler = handlerMap.get(type);

                if (handler != null) {
                    log.info("Execute Action: type={}, params={}, context={}", type, action.getActionParams(), context);
                    // 执行动作
                    handler.execute(action.getActionParams(), context);
                } else {
                    log.warn("No handler found for action type: {}", type);
                }
            } catch (Exception e) {
                log.error("Execute action failed. actionId={}", action.getActionId(), e);
            }
        }
    }
}
