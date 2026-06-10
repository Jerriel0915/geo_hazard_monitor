package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmCriteriaLog;

import java.util.List;

/**
 * 告警判据服务接口
 *
 * @author zwei
 */
public interface IAlarmCriteriaService {

    /**
     * 分页查询判据列表
     */
    List<AlarmCriteria> selectList(AlarmCriteria criteria);

    /**
     * 获取判据详情
     */
    AlarmCriteria selectById(Long id);

    /**
     * 按监测内容ID获取启用的判据
     */
    List<AlarmCriteria> selectEnabledByMonitorContentId(Long monitorContentId);

    /**
     * 按隐患点ID获取启用的判据
     */
    List<AlarmCriteria> selectEnabledByHazardPointId(Long hazardPointId);

    /**
     * 新增判据
     */
    int insert(AlarmCriteria criteria);

    /**
     * 修改判据（触发版本日志）
     */
    int update(AlarmCriteria criteria);

    /**
     * 删除判据（软删除）
     */
    int delete(Long id);

    /**
     * 启用/停用判据
     */
    int toggle(Long id, Integer isEnabled);

    /**
     * 查询判据变更日志
     */
    List<AlarmCriteriaLog> selectLogsByCriteriaId(Long criteriaId);

    /**
     * 校验判据在指定隐患点下 name 唯一
     *
     * @param name          判据名称
     * @param hazardPointId 隐患点ID（null 表示全局兜底判据）
     * @param id            排除的判据ID（新增传 0L）
     * @return true=唯一，false=已存在
     */
    boolean checkCriteriaUnique(String name, Long hazardPointId, Long id);
}
