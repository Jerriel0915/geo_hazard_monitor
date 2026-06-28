package com.zwei.iot.alarm.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.AlarmStrategyHazardPoint;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.IAlarmStrategyService;
import com.zwei.iot.alarm.service.engine.GroovyScriptExecutor;
import com.zwei.iot.alarm.service.engine.StrategyQuartzScheduler;
import com.zwei.iot.timeseries.compute.ScriptCacheOps;
import com.zwei.iot.timeseries.compute.ScriptSensorQuery;
import com.zwei.common.utils.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 综合告警策略服务实现
 *
 * @author zwei
 */
@Slf4j
@Service
public class AlarmStrategyServiceImpl implements IAlarmStrategyService {

    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor groovyScriptExecutor;
    private final ScriptCacheOps cacheOps;
    private final ScriptSensorQuery scriptSensorQuery;
    private final StrategyQuartzScheduler quartzScheduler;

    public AlarmStrategyServiceImpl(AlarmStrategyMapper strategyMapper,
                                    AlarmStrategyHazardPointMapper bindingMapper,
                                    GroovyScriptExecutor groovyScriptExecutor,
                                    ScriptCacheOps cacheOps,
                                    ScriptSensorQuery scriptSensorQuery,
                                    StrategyQuartzScheduler quartzScheduler) {
        this.strategyMapper = strategyMapper;
        this.bindingMapper = bindingMapper;
        this.groovyScriptExecutor = groovyScriptExecutor;
        this.cacheOps = cacheOps;
        this.scriptSensorQuery = scriptSensorQuery;
        this.quartzScheduler = quartzScheduler;
    }

    /**
     * 启动时批量注册所有启用的 CRON 策略到 Quartz。
     */
    @PostConstruct
    public void initCronStrategies() {
        List<AlarmStrategy> cronStrategies = strategyMapper.selectEnabledByTriggerMode("CRON");
        for (AlarmStrategy s : cronStrategies) {
            if (s.getIsEnabled() != null && s.getIsEnabled() == 1) {
                quartzScheduler.scheduleOrUpdate(s);
            }
        }
        log.info("已注册 {} 个 CRON 策略到 Quartz", cronStrategies.size());
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
    public int insert(AlarmStrategy strategy, String[] hazardPointIds) {
        if (!checkStrategyNameUnique(strategy.getName(), 0L)) {
            throw new ServiceException("新增失败，策略名称已存在");
        }
        strategy.setCreateTime(new Date());
        int rows = strategyMapper.insertStrategy(strategy);
        if (rows > 0 && hazardPointIds != null) {
            updateBindings(strategy.getId(), hazardPointIds);
        }
        if ("CRON".equals(strategy.getTriggerMode()) && strategy.getIsEnabled() != null && strategy.getIsEnabled() == 1) {
            quartzScheduler.scheduleOrUpdate(strategy);
        }
        return rows;
    }

    @Override
    @Transactional
    public int update(AlarmStrategy strategy, String[] hazardPointIds) {
        if (!checkStrategyNameUnique(strategy.getName(), strategy.getId())) {
            throw new ServiceException("修改失败，策略名称已存在");
        }
        strategy.setUpdateTime(new Date());
        int rows = strategyMapper.updateStrategy(strategy);
        if (rows > 0 && hazardPointIds != null) {
            updateBindings(strategy.getId(), hazardPointIds);
        }
        if ("CRON".equals(strategy.getTriggerMode())) {
            quartzScheduler.scheduleOrUpdate(strategy);
        } else {
            quartzScheduler.unschedule(strategy.getId());
        }
        return rows;
    }

    @Override
    @Transactional
    public int delete(Long id) {
        bindingMapper.deleteByStrategyId(id);
        quartzScheduler.unschedule(id);
        return strategyMapper.deleteStrategyById(id);
    }

    @Override
    public int toggle(Long id, Integer isEnabled) {
        AlarmStrategy update = AlarmStrategy.builder()
                .id(id).isEnabled(isEnabled)
                .updateBy(SecurityUtils.getUsername()).updateTime(new Date()).build();
        if (isEnabled == 1) quartzScheduler.resume(id);
        else quartzScheduler.pause(id);
        return strategyMapper.updateStrategy(update);
    }

    @Override
    public List<String> getScopeValues(Long strategyId) {
        return bindingMapper.selectScopeValuesByStrategyId(strategyId);
    }

    @Override
    public boolean checkStrategyNameUnique(String name, Long id) {
        return strategyMapper.checkStrategyNameUnique(name, id) == null;
    }

    @Override
    public StrategyTestRunResult testRun(Long id, StrategyTestRunRequest request) {
        StrategyTestRunResult result = new StrategyTestRunResult();
        long start = System.currentTimeMillis();

        AlarmStrategy strategy = strategyMapper.selectStrategyById(id);
        if (strategy == null) {
            result.setError("策略不存在: id=" + id);
            result.setDurationMs(System.currentTimeMillis() - start);
            return result;
        }
        if (strategy.getScriptContent() == null || strategy.getScriptContent().trim().isEmpty()) {
            result.setError("策略脚本内容为空");
            result.setDurationMs(System.currentTimeMillis() - start);
            return result;
        }

        // TODO: Task 7 will replace with full StrategyScopeResolver logic
        List<String> scopeValues = bindingMapper.selectScopeValuesByStrategyId(id);
        List<Long> hazardPointIds = scopeValues.stream()
                .filter(s -> !s.startsWith("*") && !s.startsWith("group:"))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        if (hazardPointIds.isEmpty() && strategy.getMonitorTypeId() != null) {
            hazardPointIds = strategyMapper.selectHazardPointIdsByMonitorTypeId(strategy.getMonitorTypeId());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("hazardPointIds", hazardPointIds);
        variables.put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        if (request != null && request.getMockSensorCode() != null) {
            variables.put("sensorCode", request.getMockSensorCode());
        }
        if (request != null && request.getMockDataTime() != null) {
            variables.put("dataTime", request.getMockDataTime());
        }

        Map<String, Object> tools = new HashMap<>();
        tools.put("cache", cacheOps);
        tools.put("sensor", scriptSensorQuery);

        try {
            Integer level = groovyScriptExecutor.executeWithTools(
                    strategy.getScriptContent(), variables, tools);
            result.setLevel(level);
            result.setLevelText(level != null ? AlarmConstants.resolveLevelText(level) : null);
        } catch (Exception e) {
            result.setError(e.getMessage());
        }
        result.setDurationMs(System.currentTimeMillis() - start);
        return result;
    }

    private void updateBindings(Long strategyId, String[] scopeValues) {
        bindingMapper.deleteByStrategyId(strategyId);
        for (String hpId : scopeValues) {
            AlarmStrategyHazardPoint binding = AlarmStrategyHazardPoint.builder()
                    .strategyId(strategyId)
                    .hazardPointId(hpId)
                    .createTime(new Date())
                    .build();
            bindingMapper.insertBinding(binding);
        }
    }
}
