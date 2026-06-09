package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.mapper.AlarmNotificationMapper;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 告警通知服务实现
 *
 * @author zwei
 */
@Service
public class AlarmNotificationServiceImpl implements IAlarmNotificationService {

    private final AlarmNotificationMapper notificationMapper;

    public AlarmNotificationServiceImpl(AlarmNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public List<AlarmNotification> selectByAlarmId(Long alarmId) {
        return notificationMapper.selectByAlarmId(alarmId);
    }

    @Override
    public List<AlarmNotification> selectList(AlarmNotification notification) {
        return notificationMapper.selectNotificationList(notification);
    }

    @Override
    public int createNotification(AlarmNotification notification) {
        notification.setCreateTime(new Date());
        return notificationMapper.insertNotification(notification);
    }

    @Override
    public int batchCreate(List<AlarmNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return 0;
        }
        notifications.forEach(n -> n.setCreateTime(new Date()));
        return notificationMapper.batchInsert(notifications);
    }

    @Override
    public int updateStatus(Long id, Integer status, String errorMsg) {
        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        return notificationMapper.updateStatus(id, status, now, errorMsg);
    }
}
