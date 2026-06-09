package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordLog;

import java.util.List;

/**
 * 告警记录服务接口
 *
 * @author zwei
 */
public interface IAlarmRecordService {

    /**
     * 获取待办告警分页列表
     */
    List<AlarmRecord> selectPendingList(AlarmRecord record);

    /**
     * 获取历史告警分页列表
     */
    List<AlarmRecord> selectHistoryList(AlarmRecord record);

    /**
     * 获取告警详情
     */
    AlarmRecord selectById(Long id);

    /**
     * 告警引擎调用: 创建或更新告警。
     * 去重逻辑由调用方（AlarmDedupService）在调用前执行。
     */
    AlarmRecord createOrUpdateAlarm(AlarmRecord record);

    /**
     * 处置告警 (状态流转)
     */
    int dispose(Long id, Integer newStatus, String note, String operator);

    /**
     * 批量处置
     */
    int batchDispose(Long[] ids, Integer status, String resolvedBy);

    /**
     * 获取告警状态变更日志
     */
    List<AlarmRecordLog> selectLogsByAlarmId(Long alarmId);

    /**
     * 统计待处理告警数
     */
    int countPending();

    /**
     * 统计隐患点告警数
     */
    int countByHazardPointId(Long hazardPointId);
}
