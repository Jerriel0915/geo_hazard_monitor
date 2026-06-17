package com.zwei.iot.timeseries.compute;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.domain.PropertyValue;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算属性求值主入口。
 *
 * <p>由 {@code MonitorIngestFacade.ingest()} 在 enrichProperties 之后调用,
 * 产出的 {@link PropertyValue} 列表追加到 {@code parsedMessage.properties},
 * 与固有属性同链路写入 IoTDB。
 *
 * <p>核心契约: 任何失败仅 warn 跳过, **绝不影响主链路**。
 */
@Service
public class ComputedAttributeEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ComputedAttributeEvaluator.class);

    private final IDeviceSensorQueryService sensorQuery;
    private final ComputedAttributeRegistry registry;
    private final ComputedScriptAssembler assembler;
    private final LastMessageStore lastMessageStore;
    private final GroovyScriptEngine scriptEngine;

    public ComputedAttributeEvaluator(IDeviceSensorQueryService sensorQuery,
                                       ComputedAttributeRegistry registry,
                                       ComputedScriptAssembler assembler,
                                       LastMessageStore lastMessageStore,
                                       GroovyScriptEngine scriptEngine) {
        this.sensorQuery = sensorQuery;
        this.registry = registry;
        this.assembler = assembler;
        this.lastMessageStore = lastMessageStore;
        this.scriptEngine = scriptEngine;
    }

    /**
     * 对单条 ParsedMessage 执行计算属性求值。
     *
     * @return 计算属性列表, 空列表表示无计算属性 / 全部失败 / 元数据缺失
     */
    public List<PropertyValue> evaluate(Long deviceId, String sensorCode, ParsedMessage message) {
        try {
            // 1. 取 monitorTypeId
            SensorMetadata meta = sensorQuery.requireSensorMetadata(deviceId, sensorCode);
            Long monitorTypeId = meta.monitorTypeId();
            if (monitorTypeId == null) return List.of();

            // 2. fast path
            List<ComputedAttribute> attrs = registry.getByMonitorTypeId(monitorTypeId);
            if (attrs.isEmpty()) return List.of();

            // 3. prevData
            ParsedMessageSnapshot prev = lastMessageStore.get(deviceId, sensorCode);

            // 4. 构建 curData / prevData
            Map<String, Object> curData = buildCurData(message);
            Map<String, Object> prevData = prev == null ? null : buildPrevData(prev);

            // 5. 拼装脚本
            String script = assembler.assemble(attrs);

            // 6. 执行
            Map<String, Object> results = scriptEngine.executeComputed(script, curData, prevData);

            // 7. 转 PropertyValue (null 值跳过)
            List<PropertyValue> computed = new ArrayList<>();
            for (ComputedAttribute a : attrs) {
                Object val = results.get(a.code());
                if (val == null) continue;
                computed.add(new PropertyValue(a.code(), a.name(), a.unit(), val, 0));
            }

            // 8. 总是写回 prevData(避免下次脚本看到更旧的 prev)
            Map<String, Object> mergedProps = new LinkedHashMap<>();
            for (PropertyValue p : message.properties()) {
                if (p.value() != null) mergedProps.put(p.identifier(), p.value());
            }
            for (PropertyValue p : computed) mergedProps.put(p.identifier(), p.value());
            lastMessageStore.put(deviceId, sensorCode,
                    new ParsedMessageSnapshot(message.deviceCode(), message.sensorCode(),
                            message.dataTime(), mergedProps));

            return computed;
        } catch (Exception e) {
            log.warn("ComputedAttributeEvaluator failed: deviceId={}, sensorCode={}",
                    deviceId, sensorCode, e);
            return List.of();
        }
    }

    private Map<String, Object> buildCurData(ParsedMessage msg) {
        Map<String, Object> props = new LinkedHashMap<>();
        for (PropertyValue p : msg.properties()) {
            if (p.value() != null) props.put(p.identifier(), p.value());
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", msg.deviceCode());
        data.put("sensorCode", msg.sensorCode());
        data.put("dataTime", msg.dataTime());
        data.put("properties", props);
        return data;
    }

    private Map<String, Object> buildPrevData(ParsedMessageSnapshot snap) {
        Map<String, Object> props = new LinkedHashMap<>(snap.properties());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceCode", snap.deviceCode());
        data.put("sensorCode", snap.sensorCode());
        data.put("dataTime", snap.dataTime());
        data.put("properties", props);
        return data;
    }

}
