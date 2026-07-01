package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategyVendor;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyVendorMapper {
    List<Long> selectVendorIdsByStrategyId(Long strategyId);
    List<Long> selectStrategyIdsByVendorId(Long vendorId);
    int insert(DataParseStrategyVendor relation);
    int batchInsert(@org.apache.ibatis.annotations.Param("strategyId") Long strategyId,
                    @org.apache.ibatis.annotations.Param("vendorIds") List<Long> vendorIds);
    int deleteByStrategyId(Long strategyId);
}
