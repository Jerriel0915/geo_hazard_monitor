package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategyDevice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyDeviceMapper {
    List<Long> selectDeviceIdsByStrategyId(Long strategyId);
    Long selectStrategyIdByDeviceId(Long deviceId);
    int insert(DataParseStrategyDevice relation);
    int batchInsert(@org.apache.ibatis.annotations.Param("strategyId") Long strategyId,
                    @org.apache.ibatis.annotations.Param("deviceIds") List<Long> deviceIds);
    int deleteByStrategyId(Long strategyId);
}
