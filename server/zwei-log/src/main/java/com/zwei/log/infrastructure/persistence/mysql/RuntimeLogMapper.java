package com.zwei.log.infrastructure.persistence.mysql;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zwei.log.api.dto.RuntimeLogQuery;
import com.zwei.log.domain.model.LogRuntimeRecord;

/**
 * 运行日志Mapper
 *
 * @author zwei
 */
@Mapper
public interface RuntimeLogMapper {

    int insert(LogRuntimeRecord record);

    List<LogRuntimeRecord> selectPage(RuntimeLogQuery query);

    List<LogRuntimeRecord> selectAfterEventId(@Param("eventId") Long eventId, @Param("limit") Integer limit);
}
