package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmFeedback;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmFeedbackMapper {

    int insertFeedback(AlarmFeedback feedback);

    List<AlarmFeedback> selectByAlarmId(Long alarmId);
}
