package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategy;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DataParseStrategyMapper {
    DataParseStrategy selectById(Long id);
    List<DataParseStrategy> selectByCondition(DataParseStrategy condition);
    List<DataParseStrategy> selectBySourceType(String sourceType);
    List<DataParseStrategy> selectEnabled();
    int insert(DataParseStrategy strategy);
    int updateById(DataParseStrategy strategy);
    int deleteById(Long id);
    int updateLastRunTime(Long id);
}
