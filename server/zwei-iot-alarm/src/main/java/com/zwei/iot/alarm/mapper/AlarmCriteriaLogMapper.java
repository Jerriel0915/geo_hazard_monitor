package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmCriteriaLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmCriteriaLogMapper {

    int insertLog(AlarmCriteriaLog log);

    List<AlarmCriteriaLog> selectLogsByCriteriaId(Long criteriaId);
}
