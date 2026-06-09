package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecordLog;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmRecordLogMapper {

    int insertLog(AlarmRecordLog log);

    List<AlarmRecordLog> selectLogsByAlarmId(Long alarmId);

    int batchInsertLogs(List<AlarmRecordLog> logs);
}
