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

    /**
     * 校验判据在指定隐患点下 name 唯一
     *
     * @param name          判据名称
     * @param hazardPointId 隐患点ID（null 表示全局兜底判据）
     * @param id            排除的判据ID（新增传 0L）
     * @return 命中的判据（null 表示唯一）
     */
    AlarmCriteria checkCriteriaUnique(@org.apache.ibatis.annotations.Param("name") String name,
                                      @org.apache.ibatis.annotations.Param("hazardPointId") Long hazardPointId,
                                      @org.apache.ibatis.annotations.Param("id") Long id);

    int insertCriteria(AlarmCriteria criteria);

    int updateCriteria(AlarmCriteria criteria);

    int deleteCriteriaById(Long id);
}
