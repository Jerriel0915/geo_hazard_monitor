package com.zwei.iot.parser.mapper;

import com.zwei.iot.parser.domain.DataParseStrategy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DataParseStrategyMapper {
    DataParseStrategy selectById(Long id);
    List<DataParseStrategy> selectByCondition(@Param("keyword") String keyword,
                                              @Param("sourceType") String sourceType,
                                              @Param("status") Integer status,
                                              @Param("appScope") String appScope);
    List<DataParseStrategy> selectBySourceType(String sourceType);
    List<DataParseStrategy> selectEnabled();
    int insert(DataParseStrategy strategy);
    int updateById(DataParseStrategy strategy);
    int deleteById(Long id);
    int updateLastRunTime(Long id);
    /** 批量更新最近运行时间（每行用各自最新日志时间） */
    int batchUpdateLastRunTime(@Param("list") java.util.List<com.zwei.iot.parser.dto.LastRunTimeEntry> list);
    DataParseStrategy checkNameUnique(@Param("name") String name, @Param("id") Long id);
}
