package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;

import java.util.List;

/**
 * 综合告警策略服务接口
 *
 * @author zwei
 */
public interface IAlarmStrategyService {

    List<AlarmStrategy> selectList(AlarmStrategy strategy);

    AlarmStrategy selectById(Long id);

    int insert(AlarmStrategy strategy, String[] hazardPointIds);

    int update(AlarmStrategy strategy, String[] hazardPointIds);

    int delete(Long id);

    int toggle(Long id, Integer isEnabled);

    /**
     * 查询策略绑定的 scope values (支持 "*", "group:{id}", "{隐患点ID}")
     */
    List<String> getScopeValues(Long strategyId);

    /**
     * 校验策略名称唯一
     *
     * @param name 策略名称
     * @param id   排除的策略ID（新增传 0L）
     * @return true=唯一，false=已存在
     */
    boolean checkStrategyNameUnique(String name, Long id);

    StrategyTestRunResult testRun(Long id, StrategyTestRunRequest request);
}
