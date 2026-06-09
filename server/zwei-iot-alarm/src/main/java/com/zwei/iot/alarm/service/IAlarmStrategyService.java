package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmStrategy;

import java.util.List;

/**
 * 综合告警策略服务接口
 *
 * @author zwei
 */
public interface IAlarmStrategyService {

    List<AlarmStrategy> selectList(AlarmStrategy strategy);

    AlarmStrategy selectById(Long id);

    int insert(AlarmStrategy strategy, Long[] hazardPointIds);

    int update(AlarmStrategy strategy, Long[] hazardPointIds);

    int delete(Long id);

    int toggle(Long id, Integer isEnabled);

    List<Long> getHazardPointIds(Long strategyId);
}
