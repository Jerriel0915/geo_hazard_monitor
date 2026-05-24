package com.zwei.log.infrastructure.persistence.mysql;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zwei.log.api.dto.OperationLogQuery;
import com.zwei.log.domain.model.LogOperationRecord;

/**
 * 接口调用日志Mapper
 *
 * @author zwei
 */
@Mapper
public interface OperationLogMapper {

    int insert(LogOperationRecord record);

    List<LogOperationRecord> selectPage(OperationLogQuery query);

    List<LogOperationRecord> selectAfterEventId(@Param("eventId") Long eventId, @Param("limit") Integer limit);
}
