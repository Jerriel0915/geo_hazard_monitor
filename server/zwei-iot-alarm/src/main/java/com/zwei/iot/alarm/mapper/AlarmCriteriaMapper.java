package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmCriteria;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmCriteriaMapper {

    List<AlarmCriteria> selectCriteriaList(AlarmCriteria criteria);

    List<AlarmCriteria> selectEnabledByMonitorContentId(Long monitorContentId);

    List<AlarmCriteria> selectEnabledByHazardPointId(Long hazardPointId);

    /**
     * 按监测类型加载兜底判据（hazard_point_id IS NULL）
     */
    List<AlarmCriteria> selectEnabledByMonitorTypeId(Long monitorTypeId);

    /**
     * 加载所有启用的判据（用于缓存预热）
     */
    List<AlarmCriteria> selectAllEnabled();

    AlarmCriteria selectCriteriaById(Long id);

    int insertCriteria(AlarmCriteria criteria);

    int updateCriteria(AlarmCriteria criteria);

    int deleteCriteriaById(Long id);
}
