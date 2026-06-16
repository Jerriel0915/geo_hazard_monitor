package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmFeedback;

import java.util.List;

/**
 * 告警反馈服务接口
 *
 * @author zwei
 */
public interface IAlarmFeedbackService {

    /**
     * 添加一条反馈记录
     *
     * @param alarmId  告警记录ID
     * @param content  反馈文本
     * @param files    附件 JSON
     * @param operator 反馈人
     */
    void addFeedback(Long alarmId, String content, String files, String operator);

    /**
     * 查询告警的所有反馈
     */
    List<AlarmFeedback> getFeedbacksByAlarmId(Long alarmId);
}
