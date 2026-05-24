package com.zwei.log.infrastructure.persistence.mysql;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zwei.log.api.dto.AuthLogQuery;
import com.zwei.log.domain.model.LogAuthRecord;

/**
 * 认证日志Mapper
 *
 * @author zwei
 */
@Mapper
public interface AuthLogMapper {

    int insert(LogAuthRecord record);

    List<LogAuthRecord> selectPage(AuthLogQuery query);

    List<LogAuthRecord> selectAfterEventId(@Param("eventId") Long eventId, @Param("limit") Integer limit);
}
