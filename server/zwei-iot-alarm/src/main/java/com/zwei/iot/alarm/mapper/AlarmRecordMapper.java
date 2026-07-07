package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AlarmRecordMapper {

    List<AlarmRecord> selectRecordList(AlarmRecord record);

    List<AlarmRecord> selectPendingRecords(AlarmRecord record);

    List<AlarmRecord> selectHistoryRecords(AlarmRecord record);

    AlarmRecord selectRecordById(Long id);

    /**
     * 查询同一判据+隐患点下非终态的告警（用于去重判断）。
     */
    AlarmRecord selectActiveByCriteria(@Param("criteriaId") Long criteriaId,
                                       @Param("hazardPointId") Long hazardPointId);

    /**
     * 查询同一策略+隐患点下非终态的告警（COMPREHENSIVE 去重）
     */
    AlarmRecord selectActiveByStrategy(@Param("strategyId") Long strategyId,
                                       @Param("hazardPointId") Long hazardPointId);

    int insertRecord(AlarmRecord record);

    int updateRecord(AlarmRecord record);

    int updateTriggerCount(@Param("id") Long id,
                           @Param("lastTriggerTime") String lastTriggerTime,
                           @Param("triggerCount") Integer triggerCount,
                           @Param("alarmMessage") String alarmMessage,
                           @Param("currentValue") String currentValue);

    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("statusName") String statusName,
                     @Param("resolvedBy") String resolvedBy,
                     @Param("resolvedAt") String resolvedAt,
                     @Param("resolutionNote") String resolutionNote);

    int batchUpdateStatus(@Param("ids") Long[] ids,
                          @Param("status") Integer status,
                          @Param("statusName") String statusName,
                          @Param("resolvedBy") String resolvedBy,
                          @Param("resolvedAt") String resolvedAt);

    int countByStatus(@Param("status") Integer status);

    int countByHazardPointId(@Param("hazardPointId") Long hazardPointId);

    /** 按告警等级统计待处理告警数量 (status 1=待处理 2=处理中) */
    List<Map<String, Object>> countPendingByLevel();

    /** 按月+等级统计告警数量 (近N个月) */
    List<Map<String, Object>> selectMonthlyLevelCounts(@Param("months") int months);

    /** 批量查询隐患点是否有待处理告警 (返回有告警的 hpId 列表) */
    List<Map<String, Object>> countPendingByHazardPointIds(@Param("ids") List<Long> hazardPointIds);

    /** 按监测类型统计待处理告警数量（JOIN trigger_detail → alarm_record → device_sensor → monitor_type） */
    List<Map<String, Object>> countPendingByMonitorType();

    /** 按等级统计触发次数（alarm_record_trigger_detail，全部触发，不限状态） */
    List<Map<String, Object>> countTriggerByLevel();

    /** 按监测类型统计触发次数（alarm_record_trigger_detail JOIN，全部触发，不限状态） */
    List<Map<String, Object>> countTriggerByMonitorType();

    /** 按隐患点统计待处理告警触发次数（JOIN trigger_detail → alarm_record），按次数降序 */
    List<Map<String, Object>> countTriggerByHazardPoint(@Param("limit") int limit);

    /** 告警总览统计：pendingCount / historyCount / totalCount / recentThreeMonthsCount */
    Map<String, Object> countOverview();

    /**
     * 更新告警等级 (再次触发且等级变化时调用)。
     */
    int updateAlarmLevel(@Param("id") Long id,
                         @Param("alarmLevel") Integer alarmLevel,
                         @Param("alarmLevelText") String alarmLevelText,
                         @Param("lastTriggerTime") String lastTriggerTime,
                         @Param("triggerCount") Integer triggerCount,
                         @Param("alarmMessage") String alarmMessage,
                         @Param("currentValue") String currentValue);

    /**
     * 按隐患点+时间窗查询告警记录 (供 report 汇总)。
     */
    List<AlarmRecord> selectByHazardPointAndTime(@Param("hazardPointId") Long hazardPointId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /**
     * 按隐患点+时间窗查询最近 limit 条告警 (供 report Top N 展示)。
     */
    List<AlarmRecord> selectTopByHazardPointAndTime(@Param("hazardPointId") Long hazardPointId,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end,
                                                     @Param("limit") int limit);
}
