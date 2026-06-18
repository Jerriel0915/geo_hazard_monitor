package com.zwei.datashare.mapper;

import com.zwei.datashare.domain.ShareStrategyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareStrategyLogMapper {

    int insert(ShareStrategyLog log);

    List<ShareStrategyLog> selectByStrategyId(@Param("strategyId") Long strategyId);

    int deleteByStrategyId(@Param("strategyId") Long strategyId);
}
