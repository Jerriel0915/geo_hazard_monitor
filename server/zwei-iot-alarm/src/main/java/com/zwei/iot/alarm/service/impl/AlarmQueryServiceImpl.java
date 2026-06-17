package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.device.domain.brief.AlarmEvent;
import com.zwei.iot.device.domain.brief.AlarmSummary;
import com.zwei.iot.device.service.IAlarmQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警查询跨模块服务实现 (供 report 模块消费)。
 */
@Service
public class AlarmQueryServiceImpl implements IAlarmQueryService {

    private final AlarmRecordMapper alarmRecordMapper;

    public AlarmQueryServiceImpl(AlarmRecordMapper alarmRecordMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
    }

    @Override
    public AlarmSummary summarizeByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end) {
        List<AlarmRecord> records = alarmRecordMapper.selectByHazardPointAndTime(hazardPointId, start, end);

        Map<Integer, Integer> levelCount = new HashMap<>();
        Map<Integer, Integer> statusCount = new HashMap<>();
        int maxLevel = 0;
        int pending = 0;

        for (AlarmRecord r : records) {
            int lvl = r.getAlarmLevel() == null ? 0 : r.getAlarmLevel();
            levelCount.merge(lvl, 1, Integer::sum);
            if (lvl > maxLevel) {
                maxLevel = lvl;
            }
            int st = r.getStatus() == null ? 0 : r.getStatus();
            statusCount.merge(st, 1, Integer::sum);
            if (st == 1) {
                pending++;
            }
        }

        return new AlarmSummary(hazardPointId, records.size(), maxLevel, pending, levelCount, statusCount);
    }

    @Override
    public List<AlarmEvent> listTopByHazardPoint(Long hazardPointId, LocalDateTime start, LocalDateTime end, int limit) {
        List<AlarmRecord> records = alarmRecordMapper.selectTopByHazardPointAndTime(hazardPointId, start, end, limit);
        return records.stream()
            .map(r -> new AlarmEvent(
                r.getId(),
                r.getFirstTriggerTime(),
                r.getLastTriggerTime(),
                r.getAlarmLevel() == null ? 0 : r.getAlarmLevel(),
                r.getAlarmLevelText(),
                r.getAlarmType(),
                r.getDeviceName(),
                r.getHazardPointName(),
                r.getAlarmMessage(),
                r.getStatus() == null ? 0 : r.getStatus(),
                r.getStatusName(),
                r.getTriggerCount() == null ? 0 : r.getTriggerCount()))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> countByMonth(Long hazardPointId, LocalDateTime start, LocalDateTime end) {
        List<AlarmRecord> records = alarmRecordMapper.selectByHazardPointAndTime(hazardPointId, start, end);
        return records.stream()
            .filter(r -> r.getFirstTriggerTime() != null)
            .collect(Collectors.groupingBy(
                r -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(r.getFirstTriggerTime());
                    return cal.get(Calendar.YEAR) + "-" +
                           String.format("%02d", cal.get(Calendar.MONTH) + 1);
                },
                TreeMap::new,
                Collectors.summingInt(r -> 1)));
    }
}
