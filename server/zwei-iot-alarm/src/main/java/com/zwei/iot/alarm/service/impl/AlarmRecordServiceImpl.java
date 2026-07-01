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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public Map<Integer, Integer> getPendingLevelStats() {
        List<Map<String, Object>> rows = alarmRecordMapper.countPendingByLevel();
        Map<Integer, Integer> result = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            result.put(i, 0);
        }
        for (Map<String, Object> row : rows) {
            Integer level = ((Number) row.get("alarmLevel")).intValue();
            Integer cnt = ((Number) row.get("cnt")).intValue();
            result.put(level, cnt);
        }
        return result;
    }

    @Override
    public Map<String, Object> getMonthlyTrend(int months) {
        List<Map<String, Object>> rows = alarmRecordMapper.selectMonthlyLevelCounts(months);

        // 生成月份序列 (近 N 个月)
        YearMonth now = YearMonth.now();
        List<String> monthLabels = new ArrayList<>(months);
        for (int i = months - 1; i >= 0; i--) {
            monthLabels.add(now.minusMonths(i).toString()); // yyyy-MM
        }

        Map<String, java.util.Map<String, Object>> monthMap = new LinkedHashMap<>();
        for (String m : monthLabels) {
            Map<String, Object> init = new HashMap<>();
            init.put("level1", 0);
            init.put("level2", 0);
            init.put("level3", 0);
            init.put("level4", 0);
            init.put("total", 0);
            monthMap.put(m, init);
        }

        for (Map<String, Object> row : rows) {
            String month = (String) row.get("month");
            if (monthMap.containsKey(month)) {
                Map<String, Object> entry = monthMap.get(month);
                entry.put("level1", ((Number) row.get("level1")).intValue());
                entry.put("level2", ((Number) row.get("level2")).intValue());
                entry.put("level3", ((Number) row.get("level3")).intValue());
                entry.put("level4", ((Number) row.get("level4")).intValue());
                entry.put("total", ((Number) row.get("total")).intValue());
            }
        }

        // 提取各序列
        List<Integer> level1 = new ArrayList<>(months);
        List<Integer> level2 = new ArrayList<>(months);
        List<Integer> level3 = new ArrayList<>(months);
        List<Integer> level4 = new ArrayList<>(months);
        List<Integer> total = new ArrayList<>(months);

        for (Map<String, Object> entry : monthMap.values()) {
            level1.add((Integer) entry.get("level1"));
            level2.add((Integer) entry.get("level2"));
            level3.add((Integer) entry.get("level3"));
            level4.add((Integer) entry.get("level4"));
            total.add((Integer) entry.get("total"));
        }

        // 简单线性回归预测未来2个月 (基于 total 序列)
        List<Integer> forecastTotal = linearForecast(total, 2);
        List<Integer> forecastLevel1 = new ArrayList<>();
        List<Integer> forecastLevel2 = new ArrayList<>();
        List<Integer> forecastLevel3 = new ArrayList<>();
        List<Integer> forecastLevel4 = new ArrayList<>();

        // 按最后一个月各等级占比分摊预测值
        int lastTotal = total.get(total.size() - 1);
        if (lastTotal > 0) {
            double r1 = (double) level1.get(level1.size() - 1) / lastTotal;
            double r2 = (double) level2.get(level2.size() - 1) / lastTotal;
            double r3 = (double) level3.get(level3.size() - 1) / lastTotal;
            double r4 = (double) level4.get(level4.size() - 1) / lastTotal;
            for (int ft : forecastTotal) {
                forecastLevel1.add((int) Math.round(ft * r1));
                forecastLevel2.add((int) Math.round(ft * r2));
                forecastLevel3.add((int) Math.round(ft * r3));
                forecastLevel4.add((int) Math.round(ft * r4));
            }
        } else {
            for (int ignored : forecastTotal) {
                forecastLevel1.add(0);
                forecastLevel2.add(0);
                forecastLevel3.add(0);
                forecastLevel4.add(0);
            }
        }

        // 预测月份标签
        List<String> forecastMonths = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            forecastMonths.add(now.plusMonths(i).toString());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("months", monthLabels);
        result.put("level1", level1);
        result.put("level2", level2);
        result.put("level3", level3);
        result.put("level4", level4);
        result.put("total", total);
        result.put("forecastMonths", forecastMonths);
        result.put("forecastLevel1", forecastLevel1);
        result.put("forecastLevel2", forecastLevel2);
        result.put("forecastLevel3", forecastLevel3);
        result.put("forecastLevel4", forecastLevel4);
        result.put("forecastTotal", forecastTotal);
        return result;
    }

    /** 简单线性回归，预测未来 n 个点 */
    private static List<Integer> linearForecast(List<Integer> values, int forecastCount) {
        int n = values.size();
        if (n < 2) {
            List<Integer> result = new ArrayList<>();
            int last = n > 0 ? values.get(n - 1) : 0;
            for (int i = 0; i < forecastCount; i++) result.add(Math.max(0, last));
            return result;
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < forecastCount; i++) {
            double predicted = intercept + slope * (n + i);
            result.add(Math.max(0, (int) Math.round(predicted)));
        }
        return result;
    }

    @Override
    public Map<String, Object> getOverview() {
        return alarmRecordMapper.countOverview();
    }

    @Override
    public List<Map<String, Object>> getSourceStats() {
        List<Map<String, Object>> rows = alarmRecordMapper.countPendingByMonitorType();
        int total = rows.stream().mapToInt(r -> ((Number) r.get("count")).intValue()).sum();
        for (Map<String, Object> row : rows) {
            int cnt = ((Number) row.get("count")).intValue();
            double rate = total > 0 ? Math.round(cnt * 1000.0 / total) / 10.0 : 0;
            row.put("rate", rate);
        }
        return rows;
    }
}
