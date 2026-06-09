package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordLog;
import com.zwei.iot.alarm.mapper.AlarmRecordLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 告警记录服务实现
 *
 * @author zwei
 */
@Service
public class AlarmRecordServiceImpl implements IAlarmRecordService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmRecordLogMapper alarmRecordLogMapper;

    public AlarmRecordServiceImpl(AlarmRecordMapper alarmRecordMapper,
                                  AlarmRecordLogMapper alarmRecordLogMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.alarmRecordLogMapper = alarmRecordLogMapper;
    }

    @Override
    public List<AlarmRecord> selectPendingList(AlarmRecord record) {
        return alarmRecordMapper.selectPendingRecords(record);
    }

    @Override
    public List<AlarmRecord> selectHistoryList(AlarmRecord record) {
        return alarmRecordMapper.selectHistoryRecords(record);
    }

    @Override
    public AlarmRecord selectById(Long id) {
        return alarmRecordMapper.selectRecordById(id);
    }

    @Override
    public AlarmRecord createOrUpdateAlarm(AlarmRecord record) {
        // 去重: 同一源(criteria/strategy)+隐患点下是否已有非终态告警
        AlarmRecord existing = null;
        if (record.getCriteriaId() != null) {
            existing = alarmRecordMapper.selectActiveByCriteria(
                    record.getCriteriaId(), record.getHazardPointId());
        } else if (record.getStrategyId() != null) {
            existing = alarmRecordMapper.selectActiveByStrategy(
                    record.getStrategyId(), record.getHazardPointId());
        }

        String now = LocalDateTime.now().format(FMT);
        if (existing != null) {
            int newCount = existing.getTriggerCount() != null ? existing.getTriggerCount() + 1 : 1;
            alarmRecordMapper.updateTriggerCount(existing.getId(), now, newCount);
            return existing;
        }

        // 新告警
        record.setFirstTriggerTime(record.getCreateTime() != null ? record.getCreateTime() : new Date());
        record.setLastTriggerTime(record.getFirstTriggerTime());
        record.setTriggerCount(1);
        record.setStatus(1);
        record.setStatusName("待处理");
        alarmRecordMapper.insertRecord(record);

        AlarmRecordLog log = AlarmRecordLog.builder()
                .alarmId(record.getId())
                .toStatus(1)
                .disposalType("告警引擎自动创建")
                .operator(AlarmConstants.SYSTEM_OPERATOR)
                .note("告警引擎自动创建")
                .createTime(new Date())
                .build();
        alarmRecordLogMapper.insertLog(log);

        return record;
    }

    @Override
    public int dispose(Long id, Integer newStatus, String note, String operator) {
        AlarmRecord record = alarmRecordMapper.selectRecordById(id);
        if (record == null) {
            return 0;
        }
        int oldStatus = record.getStatus() != null ? record.getStatus() : 1;
        String statusName = resolveStatusName(newStatus);
        String now = LocalDateTime.now().format(FMT);

        int rows = alarmRecordMapper.updateStatus(id, newStatus, statusName, operator, now, note);
        if (rows > 0) {
            AlarmRecordLog log = AlarmRecordLog.builder()
                    .alarmId(id)
                    .fromStatus(oldStatus)
                    .toStatus(newStatus)
                    .disposalType(resolveDisposalType(oldStatus, newStatus))
                    .operator(operator)
                    .disposalResult(note)
                    .note(note)
                    .createTime(new Date())
                    .build();
            alarmRecordLogMapper.insertLog(log);
        }
        return rows;
    }

    @Override
    public int batchDispose(Long[] ids, Integer status, String resolvedBy) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        String statusName = resolveStatusName(status);
        String now = LocalDateTime.now().format(FMT);
        int rows = alarmRecordMapper.batchUpdateStatus(ids, status, statusName, resolvedBy, now);

        // 批量插入日志（避免 N+1）
        List<AlarmRecordLog> logs = new ArrayList<>(ids.length);
        for (Long id : ids) {
            logs.add(AlarmRecordLog.builder()
                    .alarmId(id)
                    .toStatus(status)
                    .disposalType(resolveDisposalType(null, status))
                    .operator(resolvedBy)
                    .disposalResult(status == 3 ? "批量销警" : "批量标记误报")
                    .note("批量操作")
                    .createTime(new Date())
                    .build());
        }
        alarmRecordLogMapper.batchInsertLogs(logs);

        return rows;
    }

    @Override
    public List<AlarmRecordLog> selectLogsByAlarmId(Long alarmId) {
        return alarmRecordLogMapper.selectLogsByAlarmId(alarmId);
    }

    @Override
    public int countPending() {
        return alarmRecordMapper.countByStatus(1);
    }

    @Override
    public int countByHazardPointId(Long hazardPointId) {
        return alarmRecordMapper.countByHazardPointId(hazardPointId);
    }

    private String resolveStatusName(Integer status) {
        if (status == null) return "待处理";
        return switch (status) {
            case 1 -> "待处理";
            case 2 -> "处理中";
            case 3 -> "已销警";
            case 4 -> "误报";
            default -> "待处理";
        };
    }

    /**
     * 根据状态转换返回处置类型文本。
     */
    private String resolveDisposalType(Integer fromStatus, Integer toStatus) {
        if (toStatus == null) return "未知";
        return switch (toStatus) {
            case 2 -> fromStatus == null ? "批量标记处理中" : "开始处置";
            case 3 -> fromStatus == null ? "批量销警" : "已销警";
            case 4 -> fromStatus == null ? "批量标记误报" : "标记误报";
            default -> "状态变更";
        };
    }
}
