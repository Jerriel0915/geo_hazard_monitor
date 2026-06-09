package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmCriteria;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmCriteriaMapper {

    List<AlarmCriteria> selectCriteriaList(AlarmCriteria criteria);

    List<AlarmCriteria> selectEnabledByMonitorContentId(Long monitorContentId);

    List<AlarmCriteria> selectEnabledByHazardPointId(Long hazardPointId);

    AlarmCriteria selectCriteriaById(Long id);

    int insertCriteria(AlarmCriteria criteria);

    int updateCriteria(AlarmCriteria criteria);

    int deleteCriteriaById(Long id);

    int updateVersion(@org.apache.ibatis.annotations.Param("id") Long id,
                      @org.apache.ibatis.annotations.Param("version") Integer version);
}
