package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

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
                           @Param("triggerCount") Integer triggerCount);

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
}
