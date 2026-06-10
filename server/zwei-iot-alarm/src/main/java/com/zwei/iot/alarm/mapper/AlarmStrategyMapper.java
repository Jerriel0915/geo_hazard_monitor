package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmStrategyMapper {

    List<AlarmStrategy> selectStrategyList(AlarmStrategy strategy);

    List<AlarmStrategy> selectEnabledByTriggerMode(String triggerMode);

    AlarmStrategy selectStrategyById(Long id);

    /**
     * 校验策略名称唯一
     *
     * @param name 策略名称
     * @param id   排除的策略ID（新增传 0L）
     * @return 命中的策略（null 表示唯一）
     */
    AlarmStrategy checkStrategyNameUnique(@org.apache.ibatis.annotations.Param("name") String name,
                                          @org.apache.ibatis.annotations.Param("id") Long id);

    int insertStrategy(AlarmStrategy strategy);

    int updateStrategy(AlarmStrategy strategy);

    int deleteStrategyById(Long id);

    int updateLastRunResult(@org.apache.ibatis.annotations.Param("id") Long id,
                            @org.apache.ibatis.annotations.Param("lastRunTime") String lastRunTime,
                            @org.apache.ibatis.annotations.Param("lastRunResult") String lastRunResult);

    /**
     * 根据监测类型ID查询所有关联的隐患点ID
     */
    List<Long> selectHazardPointIdsByMonitorTypeId(Long monitorTypeId);
}
