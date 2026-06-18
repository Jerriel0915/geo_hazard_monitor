package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmRecordTriggerDetailMapper {

    int insertDetail(AlarmRecordTriggerDetail detail);

    List<AlarmRecordTriggerDetail> selectByAlarmRecordId(Long alarmRecordId);
}
