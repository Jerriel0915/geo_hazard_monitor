package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseLog;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseLogMapper {
    int insert(DataParseLog log);
    List<DataParseLog> selectByCondition(Long strategyId, String logLevel, String startTime, String endTime);
    int deleteByStrategyId(Long strategyId);
}
