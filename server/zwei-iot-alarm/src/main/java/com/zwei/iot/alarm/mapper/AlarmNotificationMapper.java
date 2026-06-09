package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmNotification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmNotificationMapper {

    List<AlarmNotification> selectNotificationList(AlarmNotification notification);

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    int insertNotification(AlarmNotification notification);

    int batchInsert(List<AlarmNotification> notifications);

    int updateStatus(@org.apache.ibatis.annotations.Param("id") Long id,
                     @org.apache.ibatis.annotations.Param("status") Integer status,
                     @org.apache.ibatis.annotations.Param("sendTime") String sendTime,
                     @org.apache.ibatis.annotations.Param("errorMsg") String errorMsg);
}
