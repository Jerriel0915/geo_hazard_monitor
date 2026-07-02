package com.zwei.log.infrastructure.persistence.mysql;

import com.zwei.log.mqtt.exception.domain.ExceptionMessageLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 异常报文日志 Mapper
 *
 * @author zwei
 */
@Mapper
public interface ExceptionLogMapper {

    int insert(ExceptionMessageLog log);

    List<ExceptionMessageLog> selectByCondition(@Param("clientId") String clientId,
                                                @Param("topic") String topic,
                                                @Param("rejectReason") String rejectReason,
                                                @Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime);

    int deleteBefore(@Param("cutoffTime") Date cutoffTime, @Param("limit") int limit);
}
