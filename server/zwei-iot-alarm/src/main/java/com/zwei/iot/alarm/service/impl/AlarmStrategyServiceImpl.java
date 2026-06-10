package com.zwei.iot.alarm.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.AlarmStrategyHazardPoint;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.IAlarmStrategyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 综合告警策略服务实现
 *
 * @author zwei
 */
@Service
public class AlarmStrategyServiceImpl implements IAlarmStrategyService {

    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;

    public AlarmStrategyServiceImpl(AlarmStrategyMapper strategyMapper,
                                    AlarmStrategyHazardPointMapper bindingMapper) {
        this.strategyMapper = strategyMapper;
        this.bindingMapper = bindingMapper;
    }

    @Override
    public List<AlarmStrategy> selectList(AlarmStrategy strategy) {
        return strategyMapper.selectStrategyList(strategy);
    }

    @Override
    public AlarmStrategy selectById(Long id) {
        return strategyMapper.selectStrategyById(id);
    }

    @Override
    @Transactional
    public int insert(AlarmStrategy strategy, Long[] hazardPointIds) {
        if (!checkStrategyNameUnique(strategy.getName(), 0L)) {
            throw new ServiceException("新增失败，策略名称已存在");
        }
        strategy.setCreateTime(new Date());
        int rows = strategyMapper.insertStrategy(strategy);
        if (rows > 0 && hazardPointIds != null) {
            updateBindings(strategy.getId(), hazardPointIds);
        }
        return rows;
    }

    @Override
    @Transactional
    public int update(AlarmStrategy strategy, Long[] hazardPointIds) {
        if (!checkStrategyNameUnique(strategy.getName(), strategy.getId())) {
            throw new ServiceException("修改失败，策略名称已存在");
        }
        strategy.setUpdateTime(new Date());
        int rows = strategyMapper.updateStrategy(strategy);
        if (rows > 0 && hazardPointIds != null) {
            updateBindings(strategy.getId(), hazardPointIds);
        }
        return rows;
    }

    @Override
    public int delete(Long id) {
        return strategyMapper.deleteStrategyById(id);
    }

    @Override
    public int toggle(Long id, Integer isEnabled) {
        AlarmStrategy update = AlarmStrategy.builder()
                .id(id).isEnabled(isEnabled).updateTime(new Date()).build();
        return strategyMapper.updateStrategy(update);
    }

    @Override
    public List<Long> getHazardPointIds(Long strategyId) {
        return bindingMapper.selectHazardPointIdsByStrategyId(strategyId);
    }

    @Override
    public boolean checkStrategyNameUnique(String name, Long id) {
        return strategyMapper.checkStrategyNameUnique(name, id) == null;
    }

    private void updateBindings(Long strategyId, Long[] hazardPointIds) {
        bindingMapper.deleteByStrategyId(strategyId);
        for (Long hpId : hazardPointIds) {
            AlarmStrategyHazardPoint binding = AlarmStrategyHazardPoint.builder()
                    .strategyId(strategyId)
                    .hazardPointId(hpId)
                    .createTime(new Date())
                    .build();
            bindingMapper.insertBinding(binding);
        }
    }
}
