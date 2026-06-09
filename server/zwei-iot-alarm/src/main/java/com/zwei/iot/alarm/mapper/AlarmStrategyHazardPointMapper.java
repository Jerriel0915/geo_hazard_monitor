package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmStrategyHazardPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmStrategyHazardPointMapper {

    List<Long> selectHazardPointIdsByStrategyId(Long strategyId);

    int insertBinding(AlarmStrategyHazardPoint binding);

    int deleteByStrategyId(Long strategyId);

    int deleteBinding(@Param("strategyId") Long strategyId,
                      @Param("hazardPointId") Long hazardPointId);

    int countByStrategyId(Long strategyId);
}
