package com.zwei.iot.alarm.service.engine;

import com.zwei.common.domain.ParsedMessageSnapshot;
import com.zwei.common.domain.PropertyValue;
import com.zwei.common.event.AlarmTriggeredEvent;
import com.zwei.common.event.MonitorDataIngestedEvent;
import com.zwei.iot.alarm.config.AlarmProperties;
import org.springframework.scheduling.annotation.Async;
import com.zwei.iot.alarm.domain.AlarmConstants;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.ConditionGroup;
import com.zwei.iot.alarm.domain.LevelCondition;
import com.zwei.iot.alarm.domain.LevelConfig;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.domain.dto.DeviceBasicInfo;
import com.zwei.iot.device.service.IDeviceHazardRelationService;
import com.zwei.iot.device.service.IDeviceQueryService;
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
import java.time.Instant;
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
            "red", 1, "orange", 2, "yellow", 3, "blue", 4);

    /** bucket 4 device 维度遍历的 kind 集合，提取为常量避免热路径每次分配数组 */
    private static final String[] DEVICE_DIMENSION_KINDS = {"current", "prev"};

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
    private final IDeviceQueryService deviceQueryService;

    public AlarmEvaluationEngine(AlarmProperties properties, IAlarmRecordService alarmRecordService,
                                 IDeviceHazardRelationService hazardRelationService,
                                 IDeviceSensorQueryService sensorQueryService,
                                 IHazardPointService hazardPointService, CriteriaEvaluator criteriaEvaluator,
                                 AlarmDedupService dedupService, CriteriaCacheService criteriaCache,
                                 ApplicationEventPublisher eventPublisher,
                                 MonitorContentMapper monitorContentMapper,
                                 IDeviceQueryService deviceQueryService) {
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
        this.deviceQueryService = deviceQueryService;
    }

    @Async("alarmNotifyExecutor")
    @EventListener
    public void onMonitorDataIngested(MonitorDataIngestedEvent event) {
        if (!properties.isEnabled()) {
            log.debug("[Alarm][Skip] 告警引擎未启用 alarm.enabled=false deviceId={} sensorCode={}",
                    event.getDeviceId(), event.getSensorCode());
            return;
        }
        int propCount = event.getProperties() != null ? event.getProperties().size() : 0;
        log.debug("[Alarm][In] 接收监测数据 deviceId={} sensorId={} sensorCode={} properties={}",
                event.getDeviceId(), event.getSensorId(), event.getSensorCode(), propCount);
        try {
            evaluate(event);
        } catch (Exception e) {
            log.error("[Alarm][Error] 告警评估失败 deviceId={} sensorCode={}",
                    event.getDeviceId(), event.getSensorCode(), e);
        }
    }

    private void evaluate(MonitorDataIngestedEvent event) {
        // 一次性构建 subjectValues — 4 维度双 key (传感器模式 + 监测类型模式)
        Map<String, Object> subjectValues = new HashMap<>();
        String sensorCode = event.getSensorCode();
        String prefix = sensorCode != null ? sensorCode + "." : "";
        ParsedMessageSnapshot prev = event.getPrevSnapshot();
        long currentDataTime = event.getDataTime();

        // ── bucket 1: 本 sensorCode 的 current payload ──
        // 多类型支持: payload 值原样放入 (Number/String/Boolean)，DATETIME 由 device/packet 维度提供
        if (event.getProperties() != null) {
            for (PropertyValue pv : event.getProperties()) {
                Object v = pv.value();
                if (v == null) continue;
                String key1 = prefix + "current.payload." + pv.identifier();
                String key2 = "current.payload." + pv.identifier();
                subjectValues.put(key1, v);
                subjectValues.put(key2, v);
            }
        }

        // ── bucket 2: 本 sensorCode 的 prev payload ──
        if (prev != null && prev.properties() != null) {
            for (Map.Entry<String, Object> e : prev.properties().entrySet()) {
                Object v = e.getValue();
                if (v == null) continue;
                subjectValues.put(prefix + "prev.payload." + e.getKey(), v);
                subjectValues.put("prev.payload." + e.getKey(), v);
            }
        }

        // ── bucket 3: packet.dataTime (DATETIME，存 Instant) ──
        Instant currentInstant = Instant.ofEpochMilli(currentDataTime);
        subjectValues.put(prefix + "current.packet.dataTime", currentInstant);
        subjectValues.put("current.packet.dataTime", currentInstant);
        if (prev != null) {
            Instant prevInstant = Instant.ofEpochMilli(prev.dataTime());
            subjectValues.put(prefix + "prev.packet.dataTime", prevInstant);
            subjectValues.put("prev.packet.dataTime", prevInstant);
        }

        // ── bucket 4: device.* (onlineStatus=BOOLEAN, lastReportTime=DATETIME) ──
        DeviceBasicInfo dev = deviceQueryService.getBasicInfoById(event.getDeviceId());
        if (dev != null) {
            Integer online = dev.online() ? 1 : 0;
            // DeviceBasicInfo.lastReportAt() 返回 epoch seconds (见 DeviceQueryServiceImpl.parseTimeToEpochSeconds)
            Instant lastReport = Instant.ofEpochSecond(dev.lastReportAt());
            for (String kind : DEVICE_DIMENSION_KINDS) {
                subjectValues.put(prefix + kind + ".device.onlineStatus", online);
                subjectValues.put(prefix + kind + ".device.lastReportTime", lastReport);
                subjectValues.put(kind + ".device.onlineStatus", online);
                subjectValues.put(kind + ".device.lastReportTime", lastReport);
            }
        }

        if (subjectValues.isEmpty()) {
            log.debug("[Alarm][Skip] 报文无数值属性 deviceId={} sensorCode={}",
                    event.getDeviceId(), event.getSensorCode());
            return;
        }

        List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(
                Collections.singletonList(event.getDeviceId()));
        if (hazardPointIds.isEmpty()) {
            log.debug("[Alarm][Skip] 设备未绑定隐患点 deviceId={}", event.getDeviceId());
            return;
        }
        log.debug("[Alarm][Step] 设备绑定隐患点 deviceId={} hazardPointIds={}", event.getDeviceId(), hazardPointIds);

        // 查询传感器属性 — 遍历报文 properties，取第一个匹配且 monitorContentId 非空的（用于兜底判据路径）
        Long monitorContentId = null;
        try {
            SensorMetadata metadata = sensorQueryService.requireSensorMetadata(event.getDeviceId(), event.getSensorCode());
            Set<String> propIdentifiers = new HashSet<>();
            if (event.getProperties() != null) {
                for (PropertyValue pv : event.getProperties()) {
                    propIdentifiers.add(pv.identifier());
                }
            }
            for (SensorAttribute attr : metadata.attributes()) {
                if (propIdentifiers.contains(attr.getAttrCode()) && attr.getMonitorContentId() != null) {
                    monitorContentId = attr.getMonitorContentId();
                    log.debug("[Alarm][Step] 传感器属性匹配 attrCode={} monitorContentId={}",
                            attr.getAttrCode(), monitorContentId);
                    break;
                }
            }
            if (monitorContentId == null) {
                log.debug("[Alarm][Step] 报文属性未匹配到 monitorContentId deviceId={} sensorCode={} subjects={}",
                        event.getDeviceId(), event.getSensorCode(), subjectValues.keySet());
            }
        } catch (Exception e) {
            log.debug("[Alarm][Skip] 传感器元数据查询失败 deviceId={} sensorCode={} err={}",
                    event.getDeviceId(), event.getSensorCode(), e.getMessage());
            return;
        }

        // ── 优先级 1: 隐患点专属判据（存在则只评估它，不再走监测类型兜底） ──
        List<AlarmCriteria> hpCriteria = new ArrayList<>();
        for (Long hpId : hazardPointIds) hpCriteria.addAll(criteriaCache.getByHazardPointId(hpId));

        if (!hpCriteria.isEmpty()) {
            log.debug("[Alarm][Branch] 走隐患点专属判据 count={} hazardPointIds={}",
                    hpCriteria.size(), hazardPointIds);
            evaluateCriteria(event, subjectValues, hpCriteria, hazardPointIds, monitorContentId);
            return;
        }
        log.debug("[Alarm][Branch] 无隐患点专属判据，尝试监测类型兜底 hazardPointIds={}", hazardPointIds);

        // ── 优先级 2: 仅当无隐患点判据时，使用监测类型兜底判据 (hazard_point_id IS NULL) ──
        if (monitorContentId == null) {
            log.debug("[Alarm][Skip] monitorContentId=null 无法匹配兜底判据 deviceId={}",
                    event.getDeviceId());
            return;
        }
        Long monitorTypeId = resolveMonitorTypeId(monitorContentId);
        if (monitorTypeId == null) {
            log.debug("[Alarm][Skip] 未解析到 monitorTypeId monitorContentId={}", monitorContentId);
            return;
        }
        List<AlarmCriteria> mtCriteria = criteriaCache.getByMonitorTypeId(monitorTypeId);
        if (mtCriteria.isEmpty()) {
            log.debug("[Alarm][Skip] 未匹配监测类型兜底判据 monitorTypeId={}", monitorTypeId);
            return;
        }
        log.debug("[Alarm][Branch] 走监测类型兜底判据 count={} monitorTypeId={}",
                mtCriteria.size(), monitorTypeId);
        evaluateCriteria(event, subjectValues, mtCriteria, hazardPointIds, monitorContentId);
    }

    /**
     * 从 monitor_content 查询其所属的 monitor_type_id
     */
    private Long resolveMonitorTypeId(Long contentId) {
        try {
            MonitorContent mc = monitorContentMapper.selectMonitorContentById(contentId);
            if (mc == null) {
                log.debug("[Alarm][Skip] monitor_content 记录不存在 contentId={}", contentId);
                return null;
            }
            Long typeId = mc.getMonitorTypeId();
            if (typeId == null) {
                log.debug("[Alarm][Skip] monitor_content.monitor_type_id 为 null contentId={}", contentId);
            }
            return typeId;
        } catch (Exception e) {
            log.debug("[Alarm][Skip] 查询 monitor_content 异常 contentId={} err={}", contentId, e.getMessage());
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
    private boolean evaluateCriteria(MonitorDataIngestedEvent event,
                                     Map<String, Object> subjectValues,
                                     List<AlarmCriteria> criteriaList,
                                     List<Long> hazardPointIds,
                                     Long monitorContentId) {
        log.debug("[Alarm][Eval] 开始评估判据 count={} subjects={} monitorContentId={} hazardPointIds={}",
                criteriaList.size(), subjectValues.keySet(), monitorContentId, hazardPointIds);
        List<Candidate> candidates = new ArrayList<>();

        for (AlarmCriteria criteria : criteriaList) {
            Long effectiveHpId = criteria.getHazardPointId();
            if (effectiveHpId == null && !hazardPointIds.isEmpty()) effectiveHpId = hazardPointIds.get(0);
            if (effectiveHpId == null) {
                log.debug("[Alarm][Eval] 判据无 effectiveHpId 跳过 criteriaId={} name={}",
                        criteria.getId(), criteria.getName());
                continue;
            }

            Map<String, LevelConfig> configMap = criteriaEvaluator.parseLevelConfig(criteria.getLevelConfig());
            if (configMap.isEmpty()) {
                log.debug("[Alarm][Eval] level_config 解析为空 criteriaId={} name={} levelConfig={}",
                        criteria.getId(), criteria.getName(), criteria.getLevelConfig());
                continue;
            }
            int persistCount  = criteria.getPersistCount()  != null ? criteria.getPersistCount()  : 1;
            int silencePeriod = criteria.getSilencePeriod() != null ? criteria.getSilencePeriod() : 0;
            log.debug("[Alarm][Eval] 判据配置 criteriaId={} name={} effectiveHpId={} persistCount={} silencePeriod={} levels={}",
                    criteria.getId(), criteria.getName(), effectiveHpId, persistCount, silencePeriod, configMap.keySet());

            // 逐等级独立评估：满足累加，未满足仅重置当前等级
            for (Map.Entry<String, LevelConfig> entry : configMap.entrySet()) {
                int level = LEVEL_VALUES.getOrDefault(entry.getKey(), 0);
                if (level <= 0) {
                    log.debug("[Alarm][Eval] 未知 level key 跳过 criteriaId={} levelKey={}",
                            criteria.getId(), entry.getKey());
                    continue;
                }

                // 等级独立 persistCount/silencePeriod（前端 groups 格式），回退到 criterion 级别
                LevelConfig lc = entry.getValue();
                int effPersistCount  = lc.getPersistCount()  != null ? lc.getPersistCount()  : persistCount;
                int effSilencePeriod = lc.getSilencePeriod() != null ? lc.getSilencePeriod() : silencePeriod;

                boolean satisfied = criteriaEvaluator.evaluateLevel(lc, subjectValues);
                if (!satisfied) {
                    log.debug("[Alarm][Eval] 等级未满足 criteriaId={} level={}({}) subjects={}",
                            criteria.getId(), level, entry.getKey(), subjectValues.keySet());
                    dedupService.clearPreTrigger(criteria.getId(), effectiveHpId, level);
                    continue;
                }
                log.debug("[Alarm][Eval] 等级已满足 criteriaId={} level={}({}) subjects={}",
                        criteria.getId(), level, entry.getKey(), subjectValues.keySet());

                if (dedupService.shouldTriggerAlarm(criteria.getId(), effectiveHpId, level,
                                                    effPersistCount, effSilencePeriod)) {
                    candidates.add(new Candidate(criteria, level, effectiveHpId, entry.getKey(), lc));
                } else {
                    log.debug("[Alarm][Eval] 去重拦截未触发 criteriaId={} level={}({}) persistCount={} silencePeriod={}",
                            criteria.getId(), level, entry.getKey(), effPersistCount, effSilencePeriod);
                }
            }
        }

        if (candidates.isEmpty()) {
            log.debug("[Alarm][Eval] 评估结束未产生候选告警 subjects={}", subjectValues.keySet());
            return false;
        }

        // 候选合并：等级值越小越严重（red=1 > orange=2 > yellow=3 > blue=4），取最小值
        Candidate winner = candidates.stream()
                .min(Comparator.comparingInt(Candidate::level))
                .orElseThrow();
        log.debug("[Alarm][Eval] 候选合并 winner criteriaId={} level={} hpId={} candidates={}",
                winner.criteria.getId(), winner.level, winner.effectiveHpId,
                candidates.stream().map(c -> c.criteria.getId() + ":L" + c.level).toList());

        // 通过 winner 判据引用的主属性查 currentValue 和 attrName
        String winnerSubject = extractFirstSubject(winner.levelConfig);
        // 用于显示的 attrCode (最后一段)
        String attrCode = normalizeAttrCode(winnerSubject);
        // 用于查找的标准化 subject — winnerSubject 本身已经是用户配置的新格式 subject
        // subjectValues 已升级为 Map<String, Object>，winner 主体通常是 payload 数值
        Object rawValue = winnerSubject != null ? subjectValues.get(winnerSubject) : null;
        Double currentValue = rawValue instanceof Number n ? n.doubleValue() : null;
        String attrName = resolveAttrName(event, attrCode);

        String hpName = getHazardPointName(winner.effectiveHpId);
        String message = buildAlarmMessage(attrName,
                currentValue != null ? currentValue : 0.0, winner.levelConfig, winner.level);
        AlarmRecord record = AlarmRecord.builder()
                .hazardPointId(winner.effectiveHpId).hazardPointName(hpName)
                .deviceId(event.getDeviceId()).sensorId(event.getSensorId())
                .monitorContentId(monitorContentId)
                .alarmLevel(winner.level).alarmLevelText(AlarmConstants.resolveLevelText(winner.level))
                .alarmType("THRESHOLD").alarmMessage(message)
                .criteriaId(winner.criteria.getId())
                .currentValue(currentValue != null ? new BigDecimal(currentValue) : null)
                .createBy(AlarmConstants.SYSTEM_OPERATOR).createTime(new Date())
                .build();
        AlarmRecord saved = alarmRecordService.createOrUpdateAlarm(record);
        eventPublisher.publishEvent(new AlarmTriggeredEvent(saved.getId(), saved.getHazardPointId(),
                saved.getAlarmLevel(), saved.getAlarmType(), saved.getAlarmMessage(), saved.getTriggerReason()));
        log.info("[Alarm][Trigger] 告警触发 id={} level={} criteria={} hpId={} subject={} currentValue={} (candidates={})",
                saved.getId(), winner.level, winner.criteria.getId(), winner.effectiveHpId,
                winnerSubject, currentValue, candidates.size());
        return true;
    }

    /**
     * 构建人类可读的告警描述。
     * <p>示例：{@code 小时雨量当前值 12.0mm 超过蓝色阈值 6.0mm，触发蓝色预警}
     */
    private String buildAlarmMessage(String attrName, double currentValue, LevelConfig lc, int level) {
        String levelText = AlarmConstants.resolveLevelText(level);
        LevelCondition cond = extractFirstCondition(lc);
        if (cond == null) {
            return attrName + "当前值 " + formatValue(currentValue) + "，触发" + levelText + "预警";
        }
        String unit = cond.getUnit() != null && !cond.getUnit().isBlank() ? cond.getUnit() : "";
        String opText = operatorText(cond.getOperator());
        Object thresholdRaw = cond.getThreshold();
        Double threshold = thresholdRaw instanceof Number ? ((Number) thresholdRaw).doubleValue() : null;
        StringBuilder sb = new StringBuilder();
        sb.append(attrName).append("当前值 ").append(formatValue(currentValue));
        if (!unit.isEmpty()) sb.append(unit);
        sb.append(" ").append(opText).append(levelText).append("阈值 ");
        if (threshold != null) sb.append(formatValue(threshold));
        if (!unit.isEmpty()) sb.append(unit);
        sb.append("，触发").append(levelText).append("预警");
        return sb.toString();
    }

    /**
     * 从 LevelConfig 中提取第一个条件（兼容 groups 和 conditions 两种格式）。
     */
    private LevelCondition extractFirstCondition(LevelConfig lc) {
        if (lc == null) return null;
        if (lc.getGroups() != null) {
            for (ConditionGroup g : lc.getGroups()) {
                if (g.getConditions() != null && !g.getConditions().isEmpty()) {
                    return g.getConditions().get(0);
                }
            }
        }
        if (lc.getConditions() != null && !lc.getConditions().isEmpty()) {
            return lc.getConditions().get(0);
        }
        return null;
    }

    /**
     * 从 LevelConfig 中提取第一个条件的 subject（判据引用的主属性）。
     * <p>用于定位 AlarmRecord.currentValue 和告警消息中的属性名。
     */
    private String extractFirstSubject(LevelConfig lc) {
        LevelCondition cond = extractFirstCondition(lc);
        return cond != null ? cond.getSubject() : null;
    }

    /**
     * 从 subject 提取 attrCode 用于告警记录显示。
     * <p>
     * subject 格式: {sensorCode.}{kind}.{dimension}.{attrCode}, 取最后一段。
     * 老格式 {@code payload.current.attrCode} 不再兼容。
     */
    private String normalizeAttrCode(String subject) {
        if (subject == null) return null;
        String s = subject.trim();
        if (s.isEmpty()) return null;
        int lastDot = s.lastIndexOf('.');
        if (lastDot < 0) return s;
        return s.substring(lastDot + 1);
    }

    /**
     * 根据 subject 从报文 properties 中查属性中文名，查不到时回退为 subject。
     */
    private String resolveAttrName(MonitorDataIngestedEvent event, String attrCode) {
        if (attrCode == null) return null;
        if (event.getProperties() != null) {
            for (PropertyValue pv : event.getProperties()) {
                if (attrCode.equals(pv.identifier())) {
                    return (pv.name() != null && !pv.name().isBlank()) ? pv.name() : attrCode;
                }
            }
        }
        return attrCode;
    }

    /**
     * 运算符 → 中文描述。
     */
    private String operatorText(String operator) {
        if (operator == null) return "超过";
        return switch (operator.toUpperCase()) {
            case "GT" -> "超过";
            case "GTE" -> "达到";
            case "LT" -> "低于";
            case "LTE" -> "降至";
            case "EQ" -> "等于";
            case "BETWEEN" -> "介于";
            default -> "超过";
        };
    }

    /**
     * 数值格式化 — 去除多余小数位。
     */
    private String formatValue(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    private String getHazardPointName(Long id) {
        try {
            HazardPoint hp = hazardPointService.selectHazardPointById(id);
            return hp != null ? hp.getName() : null;
        } catch (Exception e) { return null; }
    }

    /** 候选告警（判据 + 等级 + 实际隐患点 ID + 等级 key + 等级配置） */
    private record Candidate(AlarmCriteria criteria, int level, Long effectiveHpId,
                             String levelKey, LevelConfig levelConfig) {}
}
