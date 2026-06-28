package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmStrategyHazardPoint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmStrategyHazardPointMapper {

    /**
     * 查询策略绑定的 scope values (支持 "*", "group:{id}", "{隐患点ID}")
     */
    List<String> selectScopeValuesByStrategyId(Long strategyId);

    int insertBinding(AlarmStrategyHazardPoint binding);

    int deleteByStrategyId(Long strategyId);

    int deleteBinding(@Param("strategyId") Long strategyId,
                      @Param("hazardPointId") String hazardPointId);

    int countByStrategyId(Long strategyId);
}
