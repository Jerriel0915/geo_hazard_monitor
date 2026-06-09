package com.zwei.iot.alarm.service.impl;

import com.alibaba.fastjson2.JSON;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmCriteriaLog;
import com.zwei.iot.alarm.mapper.AlarmCriteriaLogMapper;
import com.zwei.iot.alarm.mapper.AlarmCriteriaMapper;
import com.zwei.iot.alarm.service.IAlarmCriteriaService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 告警判据服务实现
 *
 * @author zwei
 */
@Service
public class AlarmCriteriaServiceImpl implements IAlarmCriteriaService {

    private final AlarmCriteriaMapper criteriaMapper;
    private final AlarmCriteriaLogMapper criteriaLogMapper;

    public AlarmCriteriaServiceImpl(AlarmCriteriaMapper criteriaMapper,
                                    AlarmCriteriaLogMapper criteriaLogMapper) {
        this.criteriaMapper = criteriaMapper;
        this.criteriaLogMapper = criteriaLogMapper;
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
        return criteriaMapper.selectEnabledByMonitorContentId(monitorContentId);
    }

    @Override
    public List<AlarmCriteria> selectEnabledByHazardPointId(Long hazardPointId) {
        return criteriaMapper.selectEnabledByHazardPointId(hazardPointId);
    }

    @Override
    public int insert(AlarmCriteria criteria) {
        criteria.setVersion(1);
        criteria.setCreateTime(new Date());
        int rows = criteriaMapper.insertCriteria(criteria);
        if (rows > 0) {
            recordLog(criteria.getId(), 1, "CREATE", null, JSON.toJSONString(criteria));
        }
        return rows;
    }

    @Override
    public int update(AlarmCriteria criteria) {
        AlarmCriteria old = criteriaMapper.selectCriteriaById(criteria.getId());
        if (old == null) {
            return 0;
        }
        int newVersion = (old.getVersion() != null ? old.getVersion() : 1) + 1;
        criteria.setVersion(newVersion);
        criteria.setUpdateTime(new Date());
        int rows = criteriaMapper.updateCriteria(criteria);
        if (rows > 0) {
            recordLog(criteria.getId(), newVersion, "UPDATE",
                    JSON.toJSONString(old), JSON.toJSONString(criteria));
        }
        return rows;
    }

    @Override
    public int delete(Long id) {
        AlarmCriteria old = criteriaMapper.selectCriteriaById(id);
        if (old == null) return 0;
        int rows = criteriaMapper.deleteCriteriaById(id);
        if (rows > 0) {
            recordLog(id, (old.getVersion() != null ? old.getVersion() : 1) + 1, "DELETE",
                    JSON.toJSONString(old), null);
        }
        return rows;
    }

    @Override
    public int toggle(Long id, Integer isEnabled) {
        AlarmCriteria old = criteriaMapper.selectCriteriaById(id);
        if (old == null) return 0;
        AlarmCriteria update = new AlarmCriteria();
        update.setId(id);
        update.setIsEnabled(isEnabled);
        update.setUpdateTime(new Date());
        int rows = criteriaMapper.updateCriteria(update);
        if (rows > 0) {
            recordLog(id, (old.getVersion() != null ? old.getVersion() : 1) + 1, "TOGGLE",
                    JSON.toJSONString(old), JSON.toJSONString(update));
        }
        return rows;
    }

    @Override
    public List<AlarmCriteriaLog> selectLogsByCriteriaId(Long criteriaId) {
        return criteriaLogMapper.selectLogsByCriteriaId(criteriaId);
    }

    private void recordLog(Long criteriaId, int version, String changeType,
                           String oldValue, String newValue) {
        AlarmCriteriaLog log = AlarmCriteriaLog.builder()
                .criteriaId(criteriaId)
                .version(version)
                .changeType(changeType)
                .oldValue(oldValue)
                .newValue(newValue)
                .createTime(new Date())
                .build();
        criteriaLogMapper.insertLog(log);
    }
}
