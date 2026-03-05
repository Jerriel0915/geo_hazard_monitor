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
    public IotRule selectIotRuleByRuleId(Long ruleId);

    /**
     * 查询规则列表
     *
     * @param iotRule 规则
     * @return 规则集合
     */
    public List<IotRule> selectIotRuleList(IotRule iotRule);

    /**
     * 新增规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    public int insertIotRule(IotRule iotRule);

    /**
     * 修改规则
     *
     * @param iotRule 规则
     * @return 结果
     */
    public int updateIotRule(IotRule iotRule);

    /**
     * 批量删除规则
     *
     * @param ruleIds 需要删除的规则ID
     * @return 结果
     */
    public int deleteIotRuleByRuleIds(Long[] ruleIds);

    /**
     * 删除规则信息
     *
     * @param ruleId 规则ID
     * @return 结果
     */
    public int deleteIotRuleByRuleId(Long ruleId);
}
