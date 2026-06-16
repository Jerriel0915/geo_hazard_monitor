package com.zwei.iot.alarm.service.engine;

import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.config.AlarmProperties;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.LevelConfig;
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
 * 告警评估引擎 V3.1 — level_config 多指标评估 + 判据缓存 + 候选合并。
 *
 * <p>核心语义：
 * <ol>
 *   <li>隐患点专属判据存在时，仅评估隐患点判据；否则才回退到监测类型兜底判据。</li>
 *   <li>逐判据、逐等级独立评估；每个等级的连续触发计数由 {@link AlarmDedupService} 维护。</li>
 *   <li>每条判据可产生多个候选等级（不同 level 各自达到 persistCount），最终取最高等级合并为单条告警。</li>
 *   <li>某等级本次未满足 → 仅重置该等级的计数器，其他等级保持。</li>
 * </ol>
 *
 * @author zwei
 */
@Service
public class AlarmEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(AlarmEvaluationEngine.class);

    /** level_config JSON 中等级 key → 数值映射（与 CriteriaEvaluator.LEVEL_VALUES 对齐） */
    private static final Map<String, Integer> LEVEL_VALUES = Map.of(
            "red", 4, "orange", 3, "yellow", 2, "blue", 1);

    private final AlarmProperties properties;
    private final IAlarmRecordService alarmRecordService;
    private final IDeviceHazardRelationService hazardRelationService;
    private final IDeviceSensorQueryService sensorQueryService;
    private final IHazardPointService hazardPointService;
    private final CriteriaEvaluator criteriaEvaluator;
    private final AlarmDedupService dedupService;
    private final CriteriaCacheService criteriaCache;
    private final ApplicationEventPublisher eventPublisher;
    private final MonitorContentMapper monitorContentMapper;

    public AlarmEvaluationEngine(AlarmProperties properties, IAlarmRecordService alarmRecordService,
                                 IDeviceHazardRelationService hazardRelationService,
                                 IDeviceSensorQueryService sensorQueryService,
                                 IHazardPointService hazardPointService, CriteriaEvaluator criteriaEvaluator,
                                 AlarmDedupService dedupService, CriteriaCacheService criteriaCache,
                                 ApplicationEventPublisher eventPublisher,
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

        // ── 优先级 1: 隐患点专属判据（存在则只评估它，不再走监测类型兜底） ──
        List<AlarmCriteria> hpCriteria = new ArrayList<>();
        for (Long hpId : hazardPointIds) hpCriteria.addAll(criteriaCache.getByHazardPointId(hpId));

        if (!hpCriteria.isEmpty()) {
            evaluateCriteria(event, hpCriteria, hazardPointIds, monitorContentId);
            return;
        }

        // ── 优先级 2: 仅当无隐患点判据时，使用监测类型兜底判据 (hazard_point_id IS NULL) ──
        if (monitorContentId != null) {
            Long monitorTypeId = resolveMonitorTypeId(monitorContentId);
            if (monitorTypeId != null) {
                List<AlarmCriteria> mtCriteria = criteriaCache.getByMonitorTypeId(monitorTypeId);
                if (!mtCriteria.isEmpty()) {
                    evaluateCriteria(event, mtCriteria, hazardPointIds, monitorContentId);
                }
            }
        }
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
     * 逐判据、逐等级独立评估；候选等级合并为最高级单条告警。
     *
     * <p>每条判据的每个等级各自维护连续触发计数：
     * <ul>
     *   <li>等级满足 → {@link AlarmDedupService#shouldTriggerAlarm} 累加计数，达到 persistCount 入候选</li>
     *   <li>等级未满足 → {@link AlarmDedupService#clearPreTrigger} 仅重置该等级计数器</li>
     * </ul>
     *
     * @return true 如果至少产生了一条候选告警
     */
    private boolean evaluateCriteria(MonitorDataIngestedEvent event, List<AlarmCriteria> criteriaList,
                                     List<Long> hazardPointIds, Long monitorContentId) {
        List<Candidate> candidates = new ArrayList<>();

        for (AlarmCriteria criteria : criteriaList) {
            Long effectiveHpId = criteria.getHazardPointId();
            if (effectiveHpId == null && !hazardPointIds.isEmpty()) effectiveHpId = hazardPointIds.get(0);
            if (effectiveHpId == null) continue;

            Map<String, Double> subjectValues = new HashMap<>();
            subjectValues.put(event.getAttrCode(), event.getValue());

            Map<String, LevelConfig> configMap = criteriaEvaluator.parseLevelConfig(criteria.getLevelConfig());
            int persistCount  = criteria.getPersistCount()  != null ? criteria.getPersistCount()  : 1;
            int silencePeriod = criteria.getSilencePeriod() != null ? criteria.getSilencePeriod() : 0;

            // 逐等级独立评估：满足累加，未满足仅重置当前等级
            for (Map.Entry<String, LevelConfig> entry : configMap.entrySet()) {
                int level = LEVEL_VALUES.getOrDefault(entry.getKey(), 0);
                if (level <= 0) continue;

                boolean satisfied = criteriaEvaluator.evaluateLevel(entry.getValue(), subjectValues);
                if (!satisfied) {
                    dedupService.clearPreTrigger(criteria.getId(), effectiveHpId, level);
                    continue;
                }
                if (dedupService.shouldTriggerAlarm(criteria.getId(), effectiveHpId, level,
                                                    persistCount, silencePeriod)) {
                    candidates.add(new Candidate(criteria, level, effectiveHpId));
                }
            }
        }

        if (candidates.isEmpty()) return false;

        // 候选合并：取最高等级；同等级取首个（max 遇到并列返回较早元素）
        Candidate winner = candidates.stream()
                .max(Comparator.comparingInt(Candidate::level))
                .orElseThrow();
        String hpName = getHazardPointName(winner.effectiveHpId);
        AlarmRecord record = AlarmRecord.builder()
                .hazardPointId(winner.effectiveHpId).hazardPointName(hpName)
                .deviceId(event.getDeviceId()).sensorId(event.getSensorId())
                .monitorContentId(monitorContentId)
                .alarmLevel(winner.level).alarmLevelText(AlarmConstants.resolveLevelText(winner.level))
                .alarmType("THRESHOLD").alarmMessage("阈值告警: " + winner.criteria.getName())
                .criteriaId(winner.criteria.getId())
                .currentValue(event.getValue() != null ? new BigDecimal(event.getValue()) : null)
                .createBy(AlarmConstants.SYSTEM_OPERATOR).createTime(new Date())
                .build();
        AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
        eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getId(), saved.getHazardPointId(),
                saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage()));
        log.info("告警触发 id={} level={} criteria={} (candidates={})",
                saved.getId(), winner.level, winner.criteria.getId(), candidates.size());
        return true;
    }

    private String getHazardPointName(Long id) {
        try {
            HazardPoint hp = hazardPointService.selectHazardPointById(id);
            return hp != null ? hp.getName() : null;
        } catch (Exception e) { return null; }
    }

    /** 候选告警（判据 + 等级 + 实际隐患点 ID） */
    private record Candidate(AlarmCriteria criteria, int level, Long effectiveHpId) {}
}
