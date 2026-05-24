package com.zwei.log.infrastructure.persistence.mysql;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.zwei.log.domain.model.LogStreamCheckpoint;

/**
 * 日志流断点Mapper
 *
 * @author zwei
 */
@Mapper
public interface LogStreamCheckpointMapper {

    int upsert(LogStreamCheckpoint checkpoint);

    List<LogStreamCheckpoint> selectBySubscriberAndTypes(@Param("subscriberKey") String subscriberKey,
        @Param("logTypes") List<String> logTypes);

    int deleteBefore(@Param("cutoffTime") Date cutoffTime, @Param("limit") Integer limit);
}
