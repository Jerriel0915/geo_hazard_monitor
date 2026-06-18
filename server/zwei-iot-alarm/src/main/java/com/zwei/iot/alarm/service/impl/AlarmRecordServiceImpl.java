package com.zwei.iot.alarm.service.impl;

import com.zwei.iot.alarm.domain.ActionType;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.mapper.AlarmRecordActionLogMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordMapper;
import com.zwei.iot.alarm.mapper.AlarmRecordTriggerDetailMapper;
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
    private final AlarmRecordActionLogMapper actionLogMapper;
    private final AlarmRecordTriggerDetailMapper triggerDetailMapper;

    public AlarmRecordServiceImpl(AlarmRecordMapper alarmRecordMapper,
                                  AlarmRecordActionLogMapper actionLogMapper,
                                  AlarmRecordTriggerDetailMapper triggerDetailMapper) {
        this.alarmRecordMapper = alarmRecordMapper;
        this.actionLogMapper = actionLogMapper;
        this.triggerDetailMapper = triggerDetailMapper;
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
        Date nowDate = new Date();

        if (existing != null) {
            // ── 再次触发分支 ──
            int newCount = (existing.getTriggerCount() != null ? existing.getTriggerCount() : 0) + 1;
            Integer oldLevel = existing.getAlarmLevel();
            Integer newLevel = record.getAlarmLevel();
            boolean levelChanged = oldLevel != null && newLevel != null && !oldLevel.equals(newLevel);

            String currentValueStr = record.getCurrentValue() != null ? record.getCurrentValue().toPlainString() : null;

            if (levelChanged) {
                // 等级变化：更新主表 alarmLevel + triggerCount + lastTriggerTime + alarmMessage
                alarmRecordMapper.updateAlarmLevel(existing.getId(), newLevel,
                        AlarmConstants.resolveLevelText(newLevel), now, newCount,
                        record.getAlarmMessage(), currentValueStr);
                existing.setAlarmLevel(newLevel);
                existing.setAlarmMessage(record.getAlarmMessage());
            } else {
                alarmRecordMapper.updateTriggerCount(existing.getId(), now, newCount,
                        record.getAlarmMessage(), currentValueStr);
                existing.setAlarmMessage(record.getAlarmMessage());
            }

            // 写触发明细 (RE_TRIGGER 场景)
            triggerDetailMapper.insertDetail(AlarmRecordTriggerDetail.builder()
                    .alarmRecordId(existing.getId())
                    .triggerTime(nowDate)
                    .alarmLevel(newLevel)
                    .alarmType(record.getAlarmType())
                    .alarmMessage(record.getAlarmMessage())
                    .createTime(nowDate)
                    .build());

            // 写动作日志：RE_TRIGGER
            actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                    .alarmRecordId(existing.getId())
                    .actionType(ActionType.RE_TRIGGER.name())
                    .operator(AlarmConstants.SYSTEM_OPERATOR)
                    .createTime(nowDate)
                    .build());

            // 等级变化时追加 LEVEL_CHANGE 日志
            if (levelChanged) {
                actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                        .alarmRecordId(existing.getId())
                        .actionType(ActionType.LEVEL_CHANGE.name())
                        .fromValue(String.valueOf(oldLevel))
                        .toValue(String.valueOf(newLevel))
                        .operator(AlarmConstants.SYSTEM_OPERATOR)
                        .createTime(nowDate)
                        .build());
                existing.setTriggerReason("等级变化");
            } else {
                existing.setTriggerReason("超过静默期");
            }

            return existing;
        }

        // ── 新建分支 ──
        record.setFirstTriggerTime(record.getCreateTime() != null ? record.getCreateTime() : nowDate);
        record.setLastTriggerTime(record.getFirstTriggerTime());
        record.setTriggerCount(1);
        record.setStatus(1);
        record.setStatusName("待处理");
        record.setTriggerReason("首次告警");
        alarmRecordMapper.insertRecord(record);

        // 写触发明细 (CREATE 场景)
        triggerDetailMapper.insertDetail(AlarmRecordTriggerDetail.builder()
                .alarmRecordId(record.getId())
                .triggerTime(nowDate)
                .alarmLevel(record.getAlarmLevel())
                .alarmType(record.getAlarmType())
                .alarmMessage(record.getAlarmMessage())
                .createTime(nowDate)
                .build());

        // 写动作日志：CREATE (to_value=1 即初始状态"待处理")
        actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                .alarmRecordId(record.getId())
                .actionType(ActionType.CREATE.name())
                .toValue("1")
                .operator(AlarmConstants.SYSTEM_OPERATOR)
                .createTime(nowDate)
                .build());

        return record;
    }

    @Override
    public int dispose(Long id, Integer newStatus, String description, String attachments,
                       String remarks, String operator) {
        AlarmRecord record = alarmRecordMapper.selectRecordById(id);
        if (record == null) {
            return 0;
        }
        String statusName = AlarmConstants.resolveStatusName(newStatus);
        String now = LocalDateTime.now().format(FMT);
        Date nowDate = new Date();
        // 兼容旧字段 note（旧前端 / 测试）→ remarks
        String effectiveRemarks = remarks != null ? remarks : null;

        int rows = alarmRecordMapper.updateStatus(id, newStatus, statusName, operator, now, effectiveRemarks);
        if (rows > 0) {
            Integer oldStatus = record.getStatus();
            ActionType actionType = resolveDisposeActionType(newStatus);
            actionLogMapper.insertLog(AlarmRecordActionLog.builder()
                    .alarmRecordId(id)
                    .actionType(actionType.name())
                    .fromValue(oldStatus != null ? String.valueOf(oldStatus) : null)
                    .toValue(String.valueOf(newStatus))
                    .remarks(effectiveRemarks)
                    .description(description)
                    .attachments(attachments)
                    .operator(operator)
                    .createTime(nowDate)
                    .build());
        }
        return rows;
    }

    /**
     * 根据目标状态推导 dispose 动作类型。
     */
    private ActionType resolveDisposeActionType(Integer newStatus) {
        if (newStatus == null) return ActionType.FEEDBACK;
        return switch (newStatus) {
            case 2 -> ActionType.FEEDBACK;
            case 3 -> ActionType.DISPOSE_CLOSE;
            case 4 -> ActionType.DISPOSE_FALSE_ALARM;
            default -> ActionType.FEEDBACK;
        };
    }

    @Override
    public int batchDispose(Long[] ids, Integer status, String description, String attachments,
                            String remarks, String resolvedBy) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        String statusName = AlarmConstants.resolveStatusName(status);
        String now = LocalDateTime.now().format(FMT);
        Date nowDate = new Date();
        int rows = alarmRecordMapper.batchUpdateStatus(ids, status, statusName, resolvedBy, now);

        // 逐条写 action_log (每条记录的 action_type/status 一致)
        ActionType actionType = resolveDisposeActionType(status);
        List<AlarmRecordActionLog> logs = new ArrayList<>(ids.length);
        for (Long id : ids) {
            logs.add(AlarmRecordActionLog.builder()
                    .alarmRecordId(id)
                    .actionType(actionType.name())
                    .toValue(String.valueOf(status))
                    .remarks(remarks)
                    .description(description)
                    .attachments(attachments)
                    .operator(resolvedBy)
                    .createTime(nowDate)
                    .build());
        }
        actionLogMapper.batchInsertLogs(logs);

        return rows;
    }

    @Override
    public List<AlarmRecordActionLog> selectActionLogsByAlarmRecordId(Long alarmRecordId) {
        return actionLogMapper.selectLogsByAlarmRecordId(alarmRecordId);
    }

    @Override
    public List<AlarmRecordTriggerDetail> selectTriggerDetailsByAlarmRecordId(Long alarmRecordId) {
        return triggerDetailMapper.selectByAlarmRecordId(alarmRecordId);
    }

    @Override
    public int countPending() {
        return alarmRecordMapper.countByStatus(1);
    }

    @Override
    public int countByHazardPointId(Long hazardPointId) {
        return alarmRecordMapper.countByHazardPointId(hazardPointId);
    }
}
