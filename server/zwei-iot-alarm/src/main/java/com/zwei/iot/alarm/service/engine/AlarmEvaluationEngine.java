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
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.mapper.MonitorContentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 告警评估引擎 V3.0 — level_config 多指标评估 + 判据缓存。
 *
 * @author zwei
 */
@Service
public class AlarmEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(AlarmEvaluationEngine.class);

    private final AlarmProperties properties;
    private final IAlarmRecordService alarmRecordService;
    private final IDeviceHazardRelationService hazardRelationService;
    private final IDeviceSensorQueryService sensorQueryService;
    private final IHazardPointService hazardPointService;
    private final CriteriaEvaluator criteriaEvaluator;
    private final AlarmDedupService dedupService;
    private final CriteriaCacheService criteriaCache;
    private final ApplicationEventPublisher eventPublisher;
    private final AlarmStrategyMapper strategyMapper;
    private final AlarmStrategyHazardPointMapper bindingMapper;
    private final GroovyScriptExecutor groovyScriptExecutor;
    private final MonitorContentMapper monitorContentMapper;

    public AlarmEvaluationEngine(AlarmProperties properties, IAlarmRecordService alarmRecordService,
                                 IDeviceHazardRelationService hazardRelationService,
                                 IDeviceSensorQueryService sensorQueryService,
                                 IHazardPointService hazardPointService, CriteriaEvaluator criteriaEvaluator,
                                 AlarmDedupService dedupService, CriteriaCacheService criteriaCache,
                                 ApplicationEventPublisher eventPublisher,
                                 AlarmStrategyMapper strategyMapper, AlarmStrategyHazardPointMapper bindingMapper,
                                 GroovyScriptExecutor groovyScriptExecutor,
                                 MonitorContentMapper monitorContentMapper) {
        this.properties = properties;
        this.alarmRecordService = alarmRecordService;
        this.hazardRelationService = hazardRelationService;
        this.sensorQueryService = sensorQueryService;
        this.hazardPointService = hazardPointService;
        this.criteriaEvaluator = criteriaEvaluator;
        this.dedupService = dedupService;
        this.criteriaCache = criteriaCache;
        this.eventPublisher = eventPublisher;
        this.strategyMapper = strategyMapper;
        this.bindingMapper = bindingMapper;
        this.groovyScriptExecutor = groovyScriptExecutor;
        this.monitorContentMapper = monitorContentMapper;
    }

    @EventListener
    public void onMonitorDataIngested(MonitorDataIngestedEvent event) {
        if (!properties.isEnabled()) return;
        try {
            evaluate(event);
        } catch (Exception e) {
            log.error("告警评估失败 deviceId={}", event.getDeviceId(), e);
        }
    }

    private void evaluate(MonitorDataIngestedEvent event) {
        if (event.getValue() == null) {
            log.debug("null value, skip");
            return;
        }

        List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(
                Collections.singletonList(event.getDeviceId()));
        if (hazardPointIds.isEmpty()) {
            log.debug("no hazard point for deviceId={}", event.getDeviceId());
            return;
        }

        // 查询传感器属性
        Long monitorContentId = null;
        String sensorAttrCode = event.getAttrCode();
        try {
            SensorMetadata metadata = sensorQueryService.requireSensorMetadata(event.getDeviceId(), event.getSensorCode());
            for (SensorAttribute attr : metadata.attributes()) {
                if (sensorAttrCode.equals(attr.getAttrCode())) {
                    monitorContentId = attr.getMonitorContentId();
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("sensor metadata fail: {}", e.getMessage());
            return;
        }

        // ── 优先级 1: 隐患点专属判据 ──
        List<AlarmCriteria> hpCriteria = new ArrayList<>();
        for (Long hpId : hazardPointIds) hpCriteria.addAll(criteriaCache.getByHazardPointId(hpId));

        boolean hpTriggered = false;
        if (!hpCriteria.isEmpty()) {
            hpTriggered = evaluateCriteria(event, hpCriteria, hazardPointIds, monitorContentId);
        }

        // ── 优先级 2: 隐患点未触发 → 兜底使用监测类型判据 (hazard_point_id IS NULL) ──
        if (!hpTriggered && monitorContentId != null) {
            Long monitorTypeId = resolveMonitorTypeId(monitorContentId);
            if (monitorTypeId != null) {
                List<AlarmCriteria> mtCriteria = criteriaCache.getByMonitorTypeId(monitorTypeId);
                if (!mtCriteria.isEmpty()) {
                    evaluateCriteria(event, mtCriteria, hazardPointIds, monitorContentId);
                }
            }
        }

        evaluateRealtimeStrategies(event, hazardPointIds);
    }

    /**
     * 从 monitor_content 查询其所属的 monitor_type_id
     */
    private Long resolveMonitorTypeId(Long contentId) {
        try {
            MonitorContent mc = monitorContentMapper.selectMonitorContentById(contentId);
            return mc != null ? mc.getMonitorTypeId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return true 如果至少有一条判据触发了告警
     */
    private boolean evaluateCriteria(MonitorDataIngestedEvent event, List<AlarmCriteria> criteriaList,
                                     List<Long> hazardPointIds, Long monitorContentId) {
        boolean anyTriggered = false;
        for (AlarmCriteria criteria : criteriaList) {
            // 构建该判据涉及的 subject → 当前值映射
            Map<String, Double> subjectValues = new HashMap<>();
            subjectValues.put(event.getAttrCode(), event.getValue()); // 当前到达的值

            // TODO V3.1: 从 IoTDB 回查该判据 level_config 中引用的其他 subject 的最新值
            // List<String> subjects = criteriaEvaluator.extractSubjects(criteria.getLevelConfig());
            // for (String subj : subjects) { if (!subjectValues.containsKey(subj)) { ... } }

            int triggeredLevel = criteriaEvaluator.evaluate(criteria, subjectValues);
            if (triggeredLevel <= 0) continue;

            Long effectiveHpId = criteria.getHazardPointId();
            if (effectiveHpId == null && !hazardPointIds.isEmpty()) effectiveHpId = hazardPointIds.get(0);
            if (effectiveHpId == null) continue;

            int persistCount = criteria.getPersistCount() != null ? criteria.getPersistCount() : 1;
            int silencePeriod = criteria.getSilencePeriod() != null ? criteria.getSilencePeriod() : 0;
            if (!dedupService.shouldTriggerAlarm(criteria.getId(), effectiveHpId, triggeredLevel, persistCount, silencePeriod))
                continue;

            String hpName = getHazardPointName(effectiveHpId);
            AlarmRecord record = AlarmRecord.builder()
                    .hazardPointId(effectiveHpId).hazardPointName(hpName)
                    .deviceId(event.getDeviceId()).sensorId(event.getSensorId())
                    .monitorContentId(monitorContentId)
                    .alarmLevel(triggeredLevel).alarmLevelText(AlarmConstants.resolveLevelText(triggeredLevel))
                    .alarmType("THRESHOLD").alarmMessage("阈值告警: " + criteria.getName())
                    .criteriaId(criteria.getId())
                    .currentValue(event.getValue() != null ? new BigDecimal(event.getValue()) : null)
                    .createBy(AlarmConstants.SYSTEM_OPERATOR).createTime(new Date())
                    .build();
            AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
            eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getId(), saved.getHazardPointId(),
                    saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));
            log.info("告警触发 id={} level={} criteria={}", saved.getId(), triggeredLevel, criteria.getId());
            anyTriggered = true;
        }
        return anyTriggered;
    }

    private void evaluateRealtimeStrategies(MonitorDataIngestedEvent event, List<Long> hazardPointIds) {
        List<AlarmStrategy> strategies = strategyMapper.selectEnabledByTriggerMode("REALTIME");
        for (AlarmStrategy strategy : strategies) {
            if (strategy.getScriptContent() == null || strategy.getScriptContent().isEmpty()) continue;
            List<Long> effectiveHpIds = bindingMapper.selectHazardPointIdsByStrategyId(strategy.getId());
            if (effectiveHpIds.isEmpty() && strategy.getMonitorTypeId() != null)
                effectiveHpIds = strategyMapper.selectHazardPointIdsByMonitorTypeId(strategy.getMonitorTypeId());
            List<Long> intersected = new ArrayList<>(effectiveHpIds);
            intersected.retainAll(hazardPointIds);
            if (intersected.isEmpty()) continue;

            Map<String, Object> vars = new HashMap<>();
            vars.put("deviceId", event.getDeviceId());
            vars.put("sensorId", event.getSensorId());
            vars.put("sensorCode", event.getSensorCode());
            vars.put("attrCode", event.getAttrCode());
            vars.put("value", event.getValue());
            vars.put("hazardPointIds", intersected);
            vars.put("dataTime", event.getDataTime());

            Integer level = groovyScriptExecutor.execute(strategy.getScriptContent(), vars);
            if (level == null || level <= 0) continue;
            for (Long hpId : intersected) {
                AlarmRecord record = AlarmRecord.builder()
                        .hazardPointId(hpId).hazardPointName(getHazardPointName(hpId))
                        .deviceId(event.getDeviceId()).sensorId(event.getSensorId())
                        .alarmLevel(level).alarmLevelText(AlarmConstants.resolveLevelText(level))
                        .alarmType("COMPREHENSIVE").alarmMessage("综合策略: " + strategy.getName())
                        .strategyId(strategy.getId())
                        .currentValue(event.getValue() != null ? new BigDecimal(event.getValue()) : null)
                        .createBy(AlarmConstants.SYSTEM_OPERATOR).createTime(new Date())
                        .build();
                AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
                eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getId(), saved.getHazardPointId(),
                        saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));
            }
        }
    }

    private String getHazardPointName(Long id) {
        try {
            HazardPoint hp = hazardPointService.selectHazardPointById(id);
            return hp != null ? hp.getName() : null;
        } catch (Exception e) { return null; }
    }
}
