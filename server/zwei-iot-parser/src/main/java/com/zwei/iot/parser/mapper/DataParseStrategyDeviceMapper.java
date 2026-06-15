package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategyDevice;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyDeviceMapper {
    List<Long> selectDeviceIdsByStrategyId(Long strategyId);
    Long selectStrategyIdByDeviceId(Long deviceId);
    int insert(DataParseStrategyDevice relation);
    int deleteByStrategyId(Long strategyId);
}
