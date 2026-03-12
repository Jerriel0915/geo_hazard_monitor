package com.zwei.module.iot.rule.service;

import com.zwei.module.iot.rule.domain.IotRule;

import java.util.List;

/**
 * IoT规则Service接口
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
public interface IIotRuleService {
    /**
     * 查询规则
     *
     * @param ruleId 规则ID
     * @return 规则
     */
    IotRule selectIotRuleByRuleId(Long ruleId);

    /**
     * 查询规则列表
     *
     * @param iotRule 规则
     * @return 规则集合
     */
    List<IotRule> selectIotRuleList(IotRule iotRule);

    /**
     * 新增规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    int insertIotRule(IotRule iotRule);

    /**
     * 修改规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    int updateIotRule(IotRule iotRule);

    /**
     * 批量删除规则
     *
     * @param ruleIds 需要删除的规则ID
     * @return 结果
     */
    int deleteIotRuleByRuleIds(Long[] ruleIds);

    /**
     * 删除规则信息
     *
     * @param ruleId 规则ID
     * @return 结果
     */
    int deleteIotRuleByRuleId(Long ruleId);
}
