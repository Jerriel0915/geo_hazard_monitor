package com.zwei.module.iot.rule.service.impl;

import com.googlecode.aviator.AviatorEvaluator;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.DateUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.module.iot.rule.domain.IotRule;
import com.zwei.module.iot.rule.domain.IotRuleAction;
import com.zwei.module.iot.rule.mapper.IotRuleMapper;
import com.zwei.module.iot.rule.service.IIotRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * IoT规则Service业务层处理
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Service
public class IotRuleServiceImpl implements IIotRuleService {
    private final IotRuleMapper iotRuleMapper;

    private final RedisCache redisCache;

    private static final String RULE_CACHE_PREFIX = "iot:rule:product:";

    @Autowired
    IotRuleServiceImpl(IotRuleMapper iotRuleMapper, RedisCache redisCache) {
        this.iotRuleMapper = iotRuleMapper;
        this.redisCache = redisCache;
    }

    /**
     * 查询规则
     *
     * @param ruleId 规则ID
     * @return 规则
     */
    @Override
    public IotRule selectIotRuleByRuleId(Long ruleId) {
        IotRule iotRule = iotRuleMapper.selectIotRuleByRuleId(ruleId);
        if (iotRule != null) {
            iotRule.setActionList(iotRuleMapper.selectRuleActionsByRuleId(ruleId));
        }
        return iotRule;
    }

    /**
     * 查询规则列表
     *
     * @param iotRule 规则
     * @return 规则
     */
    @Override
    public List<IotRule> selectIotRuleList(IotRule iotRule) {
        return iotRuleMapper.selectIotRuleList(iotRule);
    }

    /**
     * 新增规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    @Override
    @Transactional
    public int insertIotRule(IotRule iotRule) {
        if (!IotRule.STATUS_DRAFT.equals(iotRule.getStatus())) {
            validateRuleExpression(iotRule.getRuleExpression());
        }
        iotRule.setCreateTime(DateUtils.getNowDate());
        int rows = iotRuleMapper.insertIotRule(iotRule);
        insertIotRuleAction(iotRule);
        // 清除缓存
        redisCache.deleteObject(RULE_CACHE_PREFIX + iotRule.getProductKey());
        return rows;
    }

    /**
     * 修改规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    @Override
    @Transactional
    public int updateIotRule(IotRule iotRule) {
        if (!IotRule.STATUS_DRAFT.equals(iotRule.getStatus())) {
            validateRuleExpression(iotRule.getRuleExpression());
        }
        // 获取旧规则以清除旧缓存（如果productKey改变）
        IotRule oldRule = iotRuleMapper.selectIotRuleByRuleId(iotRule.getRuleId());
        if (oldRule != null) {
            redisCache.deleteObject(RULE_CACHE_PREFIX + oldRule.getProductKey());
        }

        iotRule.setUpdateTime(DateUtils.getNowDate());
        iotRuleMapper.deleteIotRuleActionByRuleId(iotRule.getRuleId());
        insertIotRuleAction(iotRule);
        int rows = iotRuleMapper.updateIotRule(iotRule);

        // 清除新缓存
        if (iotRule.getProductKey() != null) {
            redisCache.deleteObject(RULE_CACHE_PREFIX + iotRule.getProductKey());
        }
        return rows;
    }

    /**
     * 批量删除规则
     *
     * @param ruleIds 需要删除的规则ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteIotRuleByRuleIds(Long[] ruleIds) {
        for (Long ruleId : ruleIds) {
            IotRule rule = iotRuleMapper.selectIotRuleByRuleId(ruleId);
            if (rule != null) {
                redisCache.deleteObject(RULE_CACHE_PREFIX + rule.getProductKey());
            }
            iotRuleMapper.deleteIotRuleActionByRuleId(ruleId);
        }
        return iotRuleMapper.deleteIotRuleByRuleIds(ruleIds);
    }

    /**
     * 删除规则信息
     *
     * @param ruleId 规则ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteIotRuleByRuleId(Long ruleId) {
        IotRule rule = iotRuleMapper.selectIotRuleByRuleId(ruleId);
        if (rule != null) {
            redisCache.deleteObject(RULE_CACHE_PREFIX + rule.getProductKey());
        }
        iotRuleMapper.deleteIotRuleActionByRuleId(ruleId);
        return iotRuleMapper.deleteIotRuleByRuleId(ruleId);
    }

    /**
     * 新增动作信息
     */
    public void insertIotRuleAction(IotRule iotRule) {
        List<IotRuleAction> iotRuleActionList = iotRule.getActionList();
        Long ruleId = iotRule.getRuleId();
        if (iotRuleActionList != null && !iotRuleActionList.isEmpty()) {
            for (IotRuleAction item : iotRuleActionList) {
                item.setRuleId(ruleId);
            }
            iotRuleMapper.batchInsertIotRuleAction(iotRuleActionList);
        }
    }

    /**
     * 表达式语法验证
     *
     * @param expression 表达式字符串
     */
    private void validateRuleExpression(String expression) {
        // 如果是草稿状态，不进行校验（需要在调用前判断状态，或者传入状态）
        // 这里假设调用方负责判断状态，或者修改此方法签名。
        // 为了最小化修改，我在调用处判断。
        if (StringUtils.isNotEmpty(expression)) {
            try {
                AviatorEvaluator.validate(expression);
            } catch (Exception e) {
                throw new ServiceException("规则表达式无效: " + e.getMessage());
            }
        }
    }
}
