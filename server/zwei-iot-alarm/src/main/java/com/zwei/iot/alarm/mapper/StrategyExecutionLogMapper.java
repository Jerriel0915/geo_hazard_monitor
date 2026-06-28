package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.StrategyExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface StrategyExecutionLogMapper {
    int insertLog(StrategyExecutionLog log);
    List<StrategyExecutionLog> selectByStrategyId(@Param("strategyId") Long strategyId,
                                                    @Param("offset") int offset,
                                                    @Param("limit") int limit);
    long countByStrategyId(@Param("strategyId") Long strategyId);
}
