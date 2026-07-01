package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;

import java.util.List;
import java.util.Map;

/**
 * 告警记录服务接口
 *
 * @author zwei
 */
public interface IAlarmRecordService {

    List<AlarmRecord> selectPendingList(AlarmRecord record);

    List<AlarmRecord> selectHistoryList(AlarmRecord record);

    AlarmRecord selectById(Long id);

    AlarmRecord createOrUpdateAlarm(AlarmRecord record);

    /**
     * 处置告警 (状态流转)
     *
     * @param id          告警ID
     * @param newStatus   新状态 2=处理中 3=已销警 4=误报
     * @param description 描述 (FEEDBACK 时附带)
     * @param attachments 附件 fileName (逗号分隔)
     * @param remarks     备注/反馈内容
     * @param operator    操作人
     */
    int dispose(Long id, Integer newStatus, String description, String attachments,
                String remarks, String operator);

    /**
     * 批量处置
     */
    int batchDispose(Long[] ids, Integer status, String description, String attachments,
                     String remarks, String resolvedBy);

    /** 动作日志列表 */
    List<AlarmRecordActionLog> selectActionLogsByAlarmRecordId(Long alarmRecordId);

    /** 触发明细列表 */
    List<AlarmRecordTriggerDetail> selectTriggerDetailsByAlarmRecordId(Long alarmRecordId);

    int countPending();

    int countByHazardPointId(Long hazardPointId);

    /** 按等级统计待处理告警数量 (所有待处理告警，非分页) */
    Map<Integer, Integer> getPendingLevelStats();

    /** 按月+等级统计告警趋势 (近N个月)，含2个月预测 */
    Map<String, Object> getMonthlyTrend(int months);

    /** 按监测类型统计待处理告警数量（含百分比） */
    List<Map<String, Object>> getSourceStats();

    /** 告警总览统计（待处理/历史/总计/近三月），单次查询 */
    Map<String, Object> getOverview();

    /** 高风险隐患点 Top N (按待处理告警触发次数降序) */
    List<Map<String, Object>> getHighRiskHazardPoints(int limit);
}
