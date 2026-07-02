package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface DataParseLogMapper {
    int insert(DataParseLog log);
    List<DataParseLog> selectByCondition(@Param("strategyId") Long strategyId,
                                         @Param("logLevel") String logLevel,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
    int deleteByStrategyId(Long strategyId);
    /** 查询给定策略集各自最新一条日志的 create_time（用于 last_run_time 批量回写） */
    List<Map<String, Object>> selectLatestCreateTimeByStrategyIds(@Param("strategyIds") List<Long> strategyIds);
}
