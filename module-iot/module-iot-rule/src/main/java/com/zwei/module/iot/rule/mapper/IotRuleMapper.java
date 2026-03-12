package com.zwei.module.iot.rule.mapper;

import com.zwei.module.iot.rule.domain.IotRule;
import com.zwei.module.iot.rule.domain.IotRuleAction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IoT规则Mapper接口
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@Mapper
public interface IotRuleMapper {
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
     * 根据产品Key查询启用的规则
     *
     * @param productKey 产品Key
     * @return 规则列表
     */
    List<IotRule> selectActiveRulesByProductKey(@Param("productKey") String productKey);

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
     * 删除规则
     *
     * @param ruleId 规则ID
     * @return 结果
     */
    int deleteIotRuleByRuleId(Long ruleId);

    /**
     * 批量删除规则
     *
     * @param ruleIds 需要删除的数据ID
     * @return 结果
     */
    int deleteIotRuleByRuleIds(Long[] ruleIds);

    /**
     * 根据规则ID查询动作
     *
     * @param ruleId 规则ID
     * @return 动作列表
     */
    List<IotRuleAction> selectRuleActionsByRuleId(Long ruleId);

    /**
     * 批量新增动作
     */
    int batchInsertIotRuleAction(List<IotRuleAction> actionList);

    /**
     * 删除动作
     */
    int deleteIotRuleActionByRuleId(Long ruleId);
}
