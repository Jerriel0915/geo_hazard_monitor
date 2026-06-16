package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmFeedback;
import com.zwei.iot.alarm.mapper.AlarmFeedbackMapper;
import com.zwei.iot.alarm.service.IAlarmFeedbackService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 告警反馈服务实现
 *
 * @author zwei
 */
@Service
public class AlarmFeedbackServiceImpl implements IAlarmFeedbackService {

    private final AlarmFeedbackMapper alarmFeedbackMapper;

    public AlarmFeedbackServiceImpl(AlarmFeedbackMapper alarmFeedbackMapper) {
        this.alarmFeedbackMapper = alarmFeedbackMapper;
    }

    @Override
    public void addFeedback(Long alarmId, String content, String files, String operator) {
        AlarmFeedback feedback = AlarmFeedback.builder()
                .alarmId(alarmId)
                .content(content)
                .files(files)
                .operator(operator)
                .createTime(new Date())
                .build();
        alarmFeedbackMapper.insertFeedback(feedback);
    }

    @Override
    public List<AlarmFeedback> getFeedbacksByAlarmId(Long alarmId) {
        return alarmFeedbackMapper.selectByAlarmId(alarmId);
    }
}
