package com.zwei.datashare.mapper;

import com.zwei.datashare.domain.ShareStrategy;
import com.zwei.datashare.enums.StrategyStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareStrategyMapper {

    int insert(ShareStrategy strategy);

    int updateById(ShareStrategy strategy);

    int deleteById(Long id);

    ShareStrategy selectById(Long id);

    List<ShareStrategy> selectList(@Param("name") String name, @Param("status") StrategyStatus status, @Param("method") String method);
    List<ShareStrategy> selectStrategyList(ShareStrategy entity);
    
    List<ShareStrategy> selectByStatus(@Param("status") StrategyStatus status);

    int incrementSuccessCount(@Param("id") Long id);

    int updateLastRunInfo(@Param("id") Long id, @Param("lastRunTime") String lastRunTime, @Param("lastRunStatus") String lastRunStatus);
}
