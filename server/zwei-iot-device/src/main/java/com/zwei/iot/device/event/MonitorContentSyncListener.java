package com.zwei.iot.device.event;

import com.zwei.common.event.MonitorContentChangedEvent;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.service.IMonitorContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 监测内容变更同步监听器 — 将字典变更反向同步到 sensor_attribute 和 product TSL。
 * <p>
 * 监听 {@link MonitorContentChangedEvent}，在字典事务提交后异步执行，
 * 避免阻塞字典写请求。
 *
 * @author zwei
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorContentSyncListener {

    private final DeviceSensorMapper deviceSensorMapper;
    private final SensorAttributeMapper sensorAttributeMapper;
    private final IMonitorContentService monitorContentService;
    private final IProductTslService productTslService;

    @Async("threadPoolTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onContentChanged(MonitorContentChangedEvent event) {
        Long monitorTypeId = event.getMonitorTypeId();
        if (monitorTypeId == null) {
            log.warn("忽略 monitorTypeId 为 null 的变更事件: changeType={}", event.getChangeType());
            return;
        }
        log.info("收到监测内容变更事件: monitorTypeId={}, changeType={}", monitorTypeId, event.getChangeType());

        // 1. 查询该监测类型下的最新字典内容
        List<MonitorContent> contents = monitorContentService.selectMonitorContentAll(monitorTypeId);
        Map<String, MonitorContent> contentByCode = contents.stream()
                .collect(Collectors.toMap(MonitorContent::getCode, mc -> mc, (a, b) -> a));
        Set<Long> contentIdSet = contents.stream()
                .map(MonitorContent::getId)
                .collect(Collectors.toSet());
        log.debug("监测类型 {} 当前字典有 {} 条内容", monitorTypeId, contents.size());

        // 2. 查询该监测类型下的所有 sensor
        DeviceSensor query = new DeviceSensor();
        query.setMonitorTypeId(monitorTypeId);
        List<DeviceSensor> sensors = deviceSensorMapper.selectSensorList(query);
        log.info("监测类型 {} 下共 {} 个传感器需同步", monitorTypeId, sensors.size());

        // 3. 逐个传感器同步
        for (DeviceSensor sensor : sensors) {
            try {
                syncSensorAttributes(sensor, contentByCode, contentIdSet);
                // 4. 重建该设备的 product TSL
                productTslService.regenerate(sensor.getDeviceId());
                log.debug("传感器 {} (id={}) 同步完成，product regenerated for deviceId={}",
                        sensor.getSensorCode(), sensor.getId(), sensor.getDeviceId());
            } catch (Exception e) {
                log.warn("同步传感器 {} (id={}) 失败，跳过: {}", sensor.getSensorCode(), sensor.getId(), e.getMessage(), e);
            }
        }
    }

    private void syncSensorAttributes(DeviceSensor sensor,
                                      Map<String, MonitorContent> contentByCode,
                                      Set<Long> contentIdSet) {
        List<SensorAttribute> existingAttrs = sensorAttributeMapper.selectAttributeListBySensorId(sensor.getId());
        Map<String, SensorAttribute> existingByCode = existingAttrs.stream()
                .collect(Collectors.toMap(SensorAttribute::getAttrCode, a -> a, (a, b) -> a));

        // INSERT 字典新增的属性 / UPDATE 字典已修改的属性
        for (MonitorContent mc : contentByCode.values()) {
            SensorAttribute existing = existingByCode.get(mc.getCode());
            if (existing == null) {
                // 新增
                SensorAttribute attr = new SensorAttribute();
                attr.setSensorId(sensor.getId());
                attr.setMonitorContentId(mc.getId());
                attr.setAttrCode(mc.getCode());
                attr.setAttrName(mc.getName());
                attr.setUnit(mc.getUnit());
                attr.setRangeMin(mc.getRangeMin());
                attr.setRangeMax(mc.getRangeMax());
                attr.setIcon(mc.getIcon());
                sensorAttributeMapper.insertAttribute(attr);
                log.debug("传感器 {} 新增属性: code={}, name={}", sensor.getSensorCode(), mc.getCode(), mc.getName());
            } else {
                // 更新（不更新 attrCode，避免破坏 IoTDB 历史路径）
                boolean changed = false;
                SensorAttribute update = new SensorAttribute();
                update.setId(existing.getId());
                if (!equals(existing.getAttrName(), mc.getName())) {
                    update.setAttrName(mc.getName());
                    changed = true;
                }
                if (!equals(existing.getUnit(), mc.getUnit())) {
                    update.setUnit(mc.getUnit());
                    changed = true;
                }
                if (!equals(existing.getRangeMin(), mc.getRangeMin())) {
                    update.setRangeMin(mc.getRangeMin());
                    changed = true;
                }
                if (!equals(existing.getRangeMax(), mc.getRangeMax())) {
                    update.setRangeMax(mc.getRangeMax());
                    changed = true;
                }
                if (!equals(existing.getIcon(), mc.getIcon())) {
                    update.setIcon(mc.getIcon());
                    changed = true;
                }
                if (changed) {
                    sensorAttributeMapper.updateAttribute(update);
                    log.debug("传感器 {} 更新属性: code={}", sensor.getSensorCode(), mc.getCode());
                }
            }
        }

        // DELETE 字典已删除的属性（按 monitorContentId 不在当前字典中判定）
        for (SensorAttribute attr : existingAttrs) {
            if (attr.getMonitorContentId() != null && !contentIdSet.contains(attr.getMonitorContentId())) {
                sensorAttributeMapper.deleteAttributeById(attr.getId());
                log.info("传感器 {} 删除属性: code={}, name={}（字典已移除 contentId={}）",
                        sensor.getSensorCode(), attr.getAttrCode(), attr.getAttrName(), attr.getMonitorContentId());
            }
        }
    }

    private static boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
