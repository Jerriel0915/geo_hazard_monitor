package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmRecordActionLogMapper {

    int insertLog(AlarmRecordActionLog log);

    int batchInsertLogs(List<AlarmRecordActionLog> logs);

    List<AlarmRecordActionLog> selectLogsByAlarmRecordId(Long alarmRecordId);
}
