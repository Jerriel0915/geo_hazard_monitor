package com.zwei.iot.alarm.service.impl;

import com.alibaba.fastjson2.JSON;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmCriteriaLog;
import com.zwei.iot.alarm.mapper.AlarmCriteriaLogMapper;
import com.zwei.iot.alarm.mapper.AlarmCriteriaMapper;
import com.zwei.iot.alarm.service.IAlarmCriteriaService;
import com.zwei.iot.alarm.service.engine.CriteriaCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 告警判据服务实现 V3.0。
 *
 * @author zwei
 */
@Service
public class AlarmCriteriaServiceImpl implements IAlarmCriteriaService {

    private final AlarmCriteriaMapper criteriaMapper;
    private final AlarmCriteriaLogMapper criteriaLogMapper;
    private final CriteriaCacheService cacheService;

    public AlarmCriteriaServiceImpl(AlarmCriteriaMapper criteriaMapper,
                                    AlarmCriteriaLogMapper criteriaLogMapper,
                                    CriteriaCacheService cacheService) {
        this.criteriaMapper = criteriaMapper;
        this.criteriaLogMapper = criteriaLogMapper;
        this.cacheService = cacheService;
    }

    @Override
    public List<AlarmCriteria> selectList(AlarmCriteria criteria) {
        return criteriaMapper.selectCriteriaList(criteria);
    }

    @Override
    public AlarmCriteria selectById(Long id) {
        return criteriaMapper.selectCriteriaById(id);
    }

    @Override
    public List<AlarmCriteria> selectEnabledByMonitorContentId(Long monitorContentId) {
        return cacheService.getByMonitorContentId(monitorContentId);
    }

    @Override
    public List<AlarmCriteria> selectEnabledByHazardPointId(Long hazardPointId) {
        return cacheService.getByHazardPointId(hazardPointId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(AlarmCriteria criteria) {
        if (!checkCriteriaUnique(criteria.getName(), criteria.getHazardPointId(), 0L)) {
            throw new ServiceException("新增失败，该隐患点下已存在同名判据");
        }
        criteria.setVersion(1);
        criteria.setCreateTime(new Date());
        int rows = criteriaMapper.insertCriteria(criteria);
        if (rows > 0) {
            recordLog(criteria.getId(), 1, "CREATE", null, JSON.toJSONString(criteria));
            cacheService.refresh();
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(AlarmCriteria criteria) {
        if (!checkCriteriaUnique(criteria.getName(), criteria.getHazardPointId(), criteria.getId())) {
            throw new ServiceException("修改失败，该隐患点下已存在同名判据");
        }
        AlarmCriteria old = criteriaMapper.selectCriteriaById(criteria.getId());
        if (old == null) return 0;
        criteria.setUpdateTime(new Date());
        int rows = criteriaMapper.updateCriteria(criteria);
        if (rows > 0) {
            recordLog(criteria.getId(), (old.getVersion() != null ? old.getVersion() : 1) + 1, "UPDATE",
                    JSON.toJSONString(old), JSON.toJSONString(criteria));
            cacheService.refresh();
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(Long id) {
        AlarmCriteria old = criteriaMapper.selectCriteriaById(id);
        if (old == null) return 0;
        int rows = criteriaMapper.deleteCriteriaById(id);
        if (rows > 0) {
            recordLog(id, (old.getVersion() != null ? old.getVersion() : 1) + 1, "DELETE",
                    JSON.toJSONString(old), null);
            cacheService.refresh();
        }
        return rows;
    }

    @Override
    public int toggle(Long id, Integer isEnabled) {
        AlarmCriteria update = AlarmCriteria.builder().id(id).isEnabled(isEnabled).updateTime(new Date()).build();
        int rows = criteriaMapper.updateCriteria(update);
        if (rows > 0) cacheService.refresh();
        return rows;
    }

    @Override
    public List<AlarmCriteriaLog> selectLogsByCriteriaId(Long criteriaId) {
        return criteriaLogMapper.selectLogsByCriteriaId(criteriaId);
    }

    @Override
    public boolean checkCriteriaUnique(String name, Long hazardPointId, Long id) {
        return criteriaMapper.checkCriteriaUnique(name, hazardPointId, id) == null;
    }

    private void recordLog(Long criteriaId, int version, String changeType, String oldValue, String newValue) {
        criteriaLogMapper.insertLog(AlarmCriteriaLog.builder()
                .criteriaId(criteriaId).version(version).changeType(changeType)
                .oldValue(oldValue).newValue(newValue).createTime(new Date()).build());
    }
}
