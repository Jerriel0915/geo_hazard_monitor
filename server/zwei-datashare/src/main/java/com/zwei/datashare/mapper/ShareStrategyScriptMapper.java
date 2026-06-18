package com.zwei.datashare.mapper;

import com.zwei.datashare.domain.ShareStrategyScript;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ShareStrategyScriptMapper {

    int insert(ShareStrategyScript script);

    int updateById(ShareStrategyScript script);

    ShareStrategyScript selectByStrategyId(@Param("strategyId") Long strategyId);

    int deleteByStrategyId(@Param("strategyId") Long strategyId);
}
