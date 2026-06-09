package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.config.AlarmProperties;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.mapper.AlarmStrategyHazardPointMapper;
import com.zwei.iot.alarm.mapper.AlarmStrategyMapper;
import com.zwei.iot.alarm.service.IAlarmCriteriaService;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 告警评估引擎 — 监听 MonitorDataIngestedEvent 并执行阈值判据匹配。
 *
 * <h3>评估流程</h3>
 * <ol>
 *   <li>根据 deviceId 查询隐患点列表</li>
 *   <li>根据 sensorNo 查询传感器元数据（含 monitorContentId）</li>
 *   <li>加载匹配的启用判据</li>
 *   <li>逐条评估判据条件</li>
 *   <li>去重判断后创建或更新告警记录</li>
 *   <li>发布 AlarmTriggeredEvent</li>
 * </ol>
 *
 * @author zwei
 */
@Service
public class AlarmEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(AlarmEvaluationEngine.class);

    private final AlarmProperties properties;
    private final IAlarmCriteriaService alarmCriteriaService;
    private final IAlarmRecordService alarmRecordService;
    private final IDeviceHazardRelationService hazardRelationService;
    private final IDeviceSensorQueryService sensorQueryService;
    private final IHazardPointService hazardPointService;
    private final CriteriaEvaluator criteriaEvaluator;
    private final AlarmDedupService dedupService;
    private final ApplicationEventPublisher eventPublisher;
    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor groovyScriptExecutor;

    public AlarmEvaluationEngine(AlarmProperties properties,
                                 IAlarmCriteriaService alarmCriteriaService,
                                 IAlarmRecordService alarmRecordService,
                                 IDeviceHazardRelationService hazardRelationService,
                                 IDeviceSensorQueryService sensorQueryService,
                                 IHazardPointService hazardPointService,
                                 CriteriaEvaluator criteriaEvaluator,
                                 AlarmDedupService dedupService,
                                 ApplicationEventPublisher eventPublisher,
                                 AlarmStrategyMapper strategyMapper,
                                 AlarmStrategyHazardPointMapper bindingMapper,
                                 GroovyScriptExecutor groovyScriptExecutor) {
        this.properties = properties;
        this.alarmCriteriaService = alarmCriteriaService;
        this.alarmRecordService = alarmRecordService;
        this.hazardRelationService = hazardRelationService;
        this.sensorQueryService = sensorQueryService;
        this.hazardPointService = hazardPointService;
        this.criteriaEvaluator = criteriaEvaluator;
        this.dedupService = dedupService;
        this.eventPublisher = eventPublisher;
        this.strategyMapper = strategyMapper;
        this.bindingMapper = bindingMapper;
        this.groovyScriptExecutor = groovyScriptExecutor;
    }

    /**
     * 监听监测数据落库事件，执行告警判据匹配。
     */
    @EventListener
    public void onMonitorDataIngested(MonitorDataIngestedEvent event) {
        if (!properties.isEnabled()) {
            log.debug("告警引擎已禁用，跳过评估");
            return;
        }

        try {
            evaluate(event);
        } catch (Exception e) {
            log.error("告警评估失败 deviceId={} sensorNo={} attrCode={}",
                    event.getDeviceId(), event.getSensorNo(), event.getAttrCode(), e);
        }
    }

    private void evaluate(MonitorDataIngestedEvent event) {
        // 0. 空值守卫
        if (event.getValue() == null) {
            log.debug("事件测量值为null，跳过告警评估 deviceId={}", event.getDeviceId());
            return;
        }
        // 1. 查询设备关联的隐患点
        List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(
                Collections.singletonList(event.getDeviceId()));
        if (hazardPointIds.isEmpty()) {
            log.debug("设备无关联隐患点，跳过告警评估 deviceId={}", event.getDeviceId());
            return;
        }

        // 2. 查询传感器属性，获取 monitorContentId
        Long monitorContentId = null;
        try {
            SensorMetadata metadata = sensorQueryService.requireSensorMetadata(
                    event.getDeviceId(), event.getSensorNo());
            for (SensorAttribute attr : metadata.attributes()) {
                if (event.getAttrCode().equals(attr.getAttrCode())) {
                    monitorContentId = attr.getMonitorContentId();
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("传感器元数据查询失败 deviceId={} sensorNo={}: {}",
                    event.getDeviceId(), event.getSensorNo(), e.getMessage());
            return;
        }

        // 3. 收集匹配的判据
        List<AlarmCriteria> criteriaList = new java.util.ArrayList<>();

        // 按监测内容 ID 查询
        if (monitorContentId != null) {
            criteriaList.addAll(alarmCriteriaService.selectEnabledByMonitorContentId(monitorContentId));
        }

        // 按隐患点 ID 查询
        for (Long hpId : hazardPointIds) {
            criteriaList.addAll(alarmCriteriaService.selectEnabledByHazardPointId(hpId));
        }

        if (!criteriaList.isEmpty()) {
            evaluateCriteria(event, criteriaList, hazardPointIds, monitorContentId);
        }

        // 同时评估 REALTIME 综合策略（含监测类型兜底）
        evaluateRealtimeStrategies(event, hazardPointIds);
    }

    private void evaluateCriteria(MonitorDataIngestedEvent event,
                                  List<AlarmCriteria> criteriaList,
                                  List<Long> hazardPointIds,
                                  Long monitorContentId) {
        for (AlarmCriteria criteria : criteriaList) {
            int triggeredLevel = criteriaEvaluator.evaluate(criteria, event.getValue());
            if (triggeredLevel <= 0) {
                continue;
            }

            // 5. 去重判断
            Long effectiveHazardPointId = criteria.getHazardPointId();
            if (effectiveHazardPointId == null && !hazardPointIds.isEmpty()) {
                effectiveHazardPointId = hazardPointIds.get(0);
            }
            if (effectiveHazardPointId == null) {
                continue;
            }

            int persistCount = criteria.getPersistCount() != null ? criteria.getPersistCount() : 1;
            int silencePeriod = criteria.getSilencePeriod() != null ? criteria.getSilencePeriod() : 0;

            boolean shouldTrigger = dedupService.shouldTriggerAlarm(
                    criteria.getId(), effectiveHazardPointId, triggeredLevel,
                    persistCount, silencePeriod);
            if (!shouldTrigger) {
                continue;
            }

            // 6. 创建告警记录
            String hazardPointName = getHazardPointName(effectiveHazardPointId);
            String alarmMessage = buildAlarmMessage(criteria.getName(),
                    event.getAttrCode(), event.getValue(), triggeredLevel);

            AlarmRecord record = AlarmRecord.builder()
                    .hazardPointId(effectiveHazardPointId)
                    .hazardPointName(hazardPointName)
                    .deviceId(event.getDeviceId())
                    .sensorId(event.getSensorId())
                    .monitorContentId(monitorContentId)
                    .alarmLevel(triggeredLevel)
                    .alarmLevelText(AlarmConstants.resolveLevelText(triggeredLevel))
                    .alarmType("THRESHOLD")
                    .alarmMessage(alarmMessage)
                    .criteriaId(criteria.getId())
                    .currentValue(new java.math.BigDecimal(event.getValue()))
                    .thresholdValue(getThresholdValue(criteria, triggeredLevel))
                    .createBy(AlarmConstants.SYSTEM_OPERATOR)
                    .createTime(new Date())
                    .build();

            AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);

            // 7. 发布告警触发事件
            eventPublisher.publishEvent(new AlarmTriggeredEvent(
                    saved.getId(), saved.getHazardPointId(),
                    saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));

            log.info("告警触发: id={} level={} hp={} criteria={} value={}",
                    saved.getId(), triggeredLevel, effectiveHazardPointId,
                    criteria.getId(), event.getValue());
        }
    }

    private String getHazardPointName(Long hazardPointId) {
        try {
            HazardPoint hp = hazardPointService.selectHazardPointById(hazardPointId);
            return hp != null ? hp.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildAlarmMessage(String criteriaName, String attrCode, Double value, int level) {
        return String.format("%s: %s=%s, 告警等级=%s",
                criteriaName, attrCode, value, AlarmConstants.resolveLevelText(level));
    }

    private java.math.BigDecimal getThresholdValue(AlarmCriteria criteria, int level) {
        String expr = switch (level) {
            case 1 -> criteria.getBlueExpression();
            case 2 -> criteria.getYellowExpression();
            case 3 -> criteria.getOrangeExpression();
            case 4 -> criteria.getRedExpression();
            default -> null;
        };
        if (expr != null) {
            try {
                return new java.math.BigDecimal(expr.trim());
            } catch (NumberFormatException e) {
                log.warn("阈值表达式解析失败 criteriaId={} expr={}", criteria.getId(), expr.trim());
            }
        }
        return null;
    }

    /**
     * 评估 REALTIME 模式综合策略 — 含监测类型兜底降级。
     * <p>
     * 优先级：
     * <ol>
     *   <li>直接绑定到该隐患点的策略</li>
     *   <li>无直接绑定时，通过策略的 monitor_type_id 匹配设备所属监测类型的兜底策略</li>
     * </ol>
     */
    private void evaluateRealtimeStrategies(MonitorDataIngestedEvent event, List<Long> hazardPointIds) {
        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        if (strategies.isEmpty()) {
            return;
        }

        for (AlarmStrategy strategy : strategies) {
            if (strategy.getScriptContent() == null || strategy.getScriptContent().isEmpty()) {
                continue;
            }

            // 优先级1: 直接绑定的隐患点
            List<Long> effectiveHpIds = bindingMapper.selectHazardPointIdsByStrategyId(strategy.getId());

            // 优先级2: 无直接绑定时，通过监测类型ID兜底
            if (effectiveHpIds.isEmpty() && strategy.getMonitorTypeId() != null) {
                effectiveHpIds = strategyMapper.selectHazardPointIdsByMonitorTypeId(strategy.getMonitorTypeId());
            }

            // 取交集：策略生效的隐患点 ∩ 当前设备关联的隐患点（不修改查询结果）
            List<Long> intersected = new java.util.ArrayList<>(effectiveHpIds);
            intersected.retainAll(hazardPointIds);
            effectiveHpIds = intersected;
            if (effectiveHpIds.isEmpty()) {
                continue;
            }

            // 执行 Groovy 脚本
            Map<String, Object> variables = new HashMap<>();
            variables.put("deviceId", event.getDeviceId());
            variables.put("sensorId", event.getSensorId());
            variables.put("sensorNo", event.getSensorNo());
            variables.put("attrCode", event.getAttrCode());
            variables.put("value", event.getValue());
            variables.put("hazardPointIds", effectiveHpIds);
            variables.put("dataTime", event.getDataTime());

            Integer alarmLevel = groovyScriptExecutor.execute(strategy.getScriptContent(), variables);
            if (alarmLevel == null || alarmLevel <= 0) {
                continue;
            }

            // 为每个生效的隐患点创建告警
            for (Long hpId : effectiveHpIds) {
                AlarmRecord record = AlarmRecord.builder()
                        .hazardPointId(hpId)
                        .hazardPointName(getHazardPointName(hpId))
                        .deviceId(event.getDeviceId())
                        .sensorId(event.getSensorId())
                        .alarmLevel(alarmLevel)
                        .alarmLevelText(AlarmConstants.resolveLevelText(alarmLevel))
                        .alarmType("COMPREHENSIVE")
                        .alarmMessage("综合策略告警: " + strategy.getName())
                        .strategyId(strategy.getId())
                        .currentValue(new java.math.BigDecimal(event.getValue()))
                        .createBy(AlarmConstants.SYSTEM_OPERATOR)
                        .createTime(new Date())
                        .build();

                AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
                eventPublisher.publishEvent(new AlarmTriggeredEvent(
                        saved.getId(), saved.getHazardPointId(),
                        saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));

                log.info("REALTIME综合策略告警: alarmId={} strategyId={} hpId={} level={}",
                        saved.getId(), strategy.getId(), hpId, alarmLevel);
            }
        }
    }
}
