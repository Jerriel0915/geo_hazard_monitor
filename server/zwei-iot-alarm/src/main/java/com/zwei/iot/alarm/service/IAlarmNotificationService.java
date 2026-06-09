package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmNotification;

import java.util.List;

/**
 * 告警通知服务接口
 *
 * @author zwei
 */
public interface IAlarmNotificationService {

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    List<AlarmNotification> selectList(AlarmNotification notification);

    int createNotification(AlarmNotification notification);

    int batchCreate(List<AlarmNotification> notifications);

    int updateStatus(Long id, Integer status, String errorMsg);
}
