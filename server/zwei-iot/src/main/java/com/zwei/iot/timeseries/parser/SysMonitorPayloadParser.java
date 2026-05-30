package com.zwei.iot.timeseries.parser;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.timeseries.domain.SensorMetadata;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.support.MonitorTopic;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * 通用 JSON 报文解析器。
 *
 * <p>新增通用 MQTT JSON 报文标准化解析逻辑，支持单值、多值和历史兼容结构，统一输出标准化时序点。</p>
 *
 * <p>报文结构分为两类：
 * <ol>
 *   <li>标准结构：包含 {@code version} 或 {@code data} 节点，常见于新版设备协议</li>
 *   <li>历史兼容结构：以设备ID为键的嵌套对象，常见于早期设备或特定厂商协议</li>
 * </ol>
 * </p>
 *
 * <p>解析流程：
 * <pre>
 *  1. 判断是否支持该主题（sourceType == "sys"）
 *  2. 解析原始报文为 JSON 对象
 *  3. 根据报文结构选择对应解析路径
 *  4. 将各种格式的数据统一转换为 StandardMeasurementPoint 列表
 *  </pre>
 * </p>
 *
 * <p>支持的报文格式示例：</p>
 *
 * <p><b>1. 标准单值（数值）</b> — value 为 Number，默认映射到 attrCode="value" 的属性：</p>
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "sensorNo": "1",
 *   "timestamp": 1716979200000,
 *   "data": {
 *     "time": 1716979200000,
 *     "value": 25.5
 *   }
 * }}</pre>
 *
 * <p><b>2. 标准单值（字符串）</b> — value 为不含逗号的数字字符串，同数值处理：</p>
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "sensorNo": "1",
 *   "data": { "time": 1716979200000, "value": "25.5" }
 * }}</pre>
 *
 * <p><b>3. 标准多值（对象）</b> — value 为 JSON 对象，每个 key 对应一个传感器属性 attrCode：</p>
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "sensorNo": "1",
 *   "timestamp": 1716979200000,
 *   "data": {
 *     "time": 1716979200000,
 *     "value": { "gpsTotalX": 1.2, "gpsTotalY": 2.3 }
 *   }
 * }}</pre>
 *
 * <p><b>4. 标准多值（CSV）</b> — value 为逗号分隔字符串，按顺序与传感器属性（id 升序）对齐：</p>
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "sensorNo": "1",
 *   "timestamp": 1716979200000,
 *   "data": { "time": 1716979200000, "value": "25.5,50" }
 * }}</pre>
 *
 * <p><b>5. 批量历史报文</b> — data 节点直接为以时间戳为键的映射对象：</p>
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "sensorNo": "1",
 *   "timestamp": 1716979300000,
 *   "data": { "1716979200000": 25.5, "1716979250000": 25.8 }
 * }}</pre>
 *
 * <p><b>6. 历史兼容报文</b> — 以 deviceId 为顶层键的嵌套对象，仅处理键以 _sensorNo 结尾的测点：</p>
 * <pre>{@code
 * {
 *   "101": {
 *     "temperature_01": { "1716979200000": 25.5 },
 *     "humidity_01":    { "1716979200000": 60.2 }
 *   }
 * }}</pre>
 */
@Component
public class SysMonitorPayloadParser implements MonitorPayloadParser {
    private static final String DEFAULT_ATTR_CODE = "value";

    /**
     * 判断当前解析器是否支持通用主题。
     * 仅当主题来源为 {@code sys} 时返回 true，确保与其他解析器职责清晰划分。
     *
     * @param topic 监测主题信息（包含 sourceType、deviceId、sensorNo）
     * @return 当 sourceType == "sys" 时返回 {@code true}
     */
    @Override
    public boolean supports(MonitorTopic topic) {
        return topic != null && Objects.equals("sys", topic.sourceType());
    }

    /**
     * 解析通用 JSON 报文为标准化时序点。
     *
     * <p>入口方法，根据报文顶层结构分发至标准解析或历史兼容解析路径：</p>
     * <ul>
     *   <li>若包含 {@code version} 或 {@code data} 键 → 标准结构解析</li>
     *   <li>否则 → 历史兼容结构解析</li>
     * </ul>
     *
     * @param topic    监测主题信息
     * @param message  原始报文字节数组
     * @param metadata 传感器元数据（包含属性定义列表）
     * @return 标准化时序点集合（永不返回 null）
     * @throws ServiceException 报文为空或结构不合法时抛出
     */
    @Override
    public List<StandardMeasurementPoint> parse(MonitorTopic topic, byte[] message, SensorMetadata metadata) {
        String payload = new String(message, StandardCharsets.UTF_8);
        JSONObject root = JSON.parseObject(payload);
        if (root == null || root.isEmpty()) {
            throw new ServiceException("通用报文为空");
        }
        // 根据顶层键判断报文结构类型：标准结构（含 version/data） vs 历史兼容结构
        if (root.containsKey("version") || root.containsKey("data")) {
            return parseStandardPayload(topic, root, payload, metadata);
        }
        return parseLegacyPayload(topic, root, payload, metadata);
    }

    /**
     * 解析标准结构的通用报文。
     *
     * <p>标准结构特征：顶层包含 {@code version}、{@code deviceId}、{@code sensorNo}、{@code timestamp}、{@code data}。
     * 其中 {@code data} 节点可能为：
     * <ul>
     *   <li>单值对象：含 {@code time} 和 {@code value} 键</li>
     *   <li>历史映射：以时间戳为键，监测值为值</li>
     *   <li>数组：多个 {@code time}/{@code value} 项</li>
     * </ul>
     * </p>
     *
     * <p>校验逻辑：在解析前验证 payload 中的 deviceId/sensorNo 与 topic 中声明的一致，防止数据串类别。</p>
     *
     * @param topic    监测主题信息
     * @param root     根 JSON 对象
     * @param payload  原始字符串报文（用于生成报文摘要）
     * @param metadata 传感器元数据
     * @return 标准化时序点集合
     * @throws ServiceException topic 与 payload 标识不一致或 data 节点格式不支持时抛出
     */
    private List<StandardMeasurementPoint> parseStandardPayload(MonitorTopic topic,
                                                                JSONObject root,
                                                                String payload,
                                                                SensorMetadata metadata) {
        // 校验传感器编号一致性：payload 中的 sensorNo 必须与 topic 中的一致，
        // 防止设备将 A 传感器的数据包装为 B 传感器的主题上报。
        String payloadSensorNo = root.getString("sensorNo");
        if (StringUtils.isNotBlank(payloadSensorNo) && !Objects.equals(payloadSensorNo, topic.sensorNo())) {
            throw new ServiceException("payload 中 sensorNo 与 topic 不一致");
        }
        // 从报文中提取上报时间，若无则使用当前系统时间
        long reportTime = resolveTimestamp(root.get("timestamp"), Instant.now().toEpochMilli());
        Object dataNode = root.get("data");

        // 根据 data 节点类型选择解析策略
        if (dataNode instanceof JSONObject dataObject) {
            // data 为单值对象（含 time/value） → 直接解析单点
            if (dataObject.containsKey("time") || dataObject.containsKey("value")) {
                return List.copyOf(parseDataPoint(topic, dataObject.get("time"), dataObject.get("value"), reportTime, payload, metadata));
            }
            // data 为历史映射（以时间戳为键） → 遍历拆解为多个时序点
            return parseHistoryObject(topic, dataObject, reportTime, payload, metadata);
        }
        if (dataNode instanceof JSONArray array) {
            // data 为数组 → 遍历每一项，解析为多个时序点
            List<StandardMeasurementPoint> points = new ArrayList<>();
            for (Object item : array) {
                if (!(item instanceof JSONObject itemObject)) {
                    continue;
                }
                points.addAll(parseDataPoint(topic, itemObject.get("time"), itemObject.get("value"), reportTime, payload, metadata));
            }
            return points;
        }
        throw new ServiceException("通用报文 data 节点格式不支持");
    }

    /**
     * 解析历史兼容结构报文。
     *
     * <p>历史兼容结构特征：以设备ID为顶层键，测点数据为嵌套值对象。例如：
     * {@code {"1001": {"temperature_01": {...}, "humidity_01": {...}}}}
     * </p>
     *
     * <p>解析策略：遍历设备对象下的所有测点键，仅保留与目标传感器编号匹配的条目，
     * 然后将每一项转换为一个标准时序点。</p>
     *
     * @param topic    监测主题信息
     * @param root     根 JSON 对象
     * @param payload  原始字符串报文
     * @param metadata 传感器元数据
     * @return 标准化时序点集合
     * @throws ServiceException 未识别的报文结构或无可写入点时抛出
     */
    private List<StandardMeasurementPoint> parseLegacyPayload(MonitorTopic topic,
                                                              JSONObject root,
                                                              String payload,
                                                              SensorMetadata metadata) {
        // 历史兼容结构以设备ID为键，取出对应的设备对象
        JSONObject deviceObject = root.getJSONObject(String.valueOf(metadata.deviceId()));
        if (deviceObject == null || deviceObject.isEmpty()) {
            throw new ServiceException("未识别的历史/兼容报文结构");
        }
        List<StandardMeasurementPoint> points = new ArrayList<>();
        long reportTime = Instant.now().toEpochMilli();
        for (Map.Entry<String, Object> entry : deviceObject.entrySet()) {
            String measurementKey = entry.getKey();
            // 过滤：仅保留与当前传感器编号匹配的测点（键格式：{监测项}_{sensorNo}）
            if (!measurementMatchesSensor(topic.sensorNo(), measurementKey)) {
                continue;
            }
            Object rawValue = entry.getValue();
            // 若值为时间戳映射结构（所有键均可解析为时间戳），则展开为多个单点
            if (rawValue instanceof JSONObject valueObject && looksLikeHistoryMap(valueObject)) {
                for (Map.Entry<String, Object> historyEntry : valueObject.entrySet()) {
                    points.addAll(parseDataPoint(topic, historyEntry.getKey(), historyEntry.getValue(), reportTime, payload, metadata));
                }
                continue;
            }
            // 否则直接作为单值时序点处理
            points.addAll(parseDataPoint(topic, reportTime, rawValue, reportTime, payload, metadata));
        }
        if (points.isEmpty()) {
            throw new ServiceException("兼容报文中未找到可写入的监测点");
        }
        return points;
    }

    /**
     * 将历史对象节点拆解为时序点集合。
     * 适用于 data 节点为键值对形式（时间戳 → 监测值）的场景。
     *
     * @param topic      监测主题信息
     * @param dataObject 历史数据对象（键为时间戳，值为监测数据）
     * @param reportTime 上报时间（用于无法解析单个时间点时的兜底）
     * @param payload    原始字符串报文
     * @param metadata   传感器元数据
     * @return 标准化时序点集合
     */
    private List<StandardMeasurementPoint> parseHistoryObject(MonitorTopic topic,
                                                              JSONObject dataObject,
                                                              long reportTime,
                                                              String payload,
                                                              SensorMetadata metadata) {
        List<StandardMeasurementPoint> points = new ArrayList<>();
        for (Map.Entry<String, Object> entry : dataObject.entrySet()) {
            points.addAll(parseDataPoint(topic, entry.getKey(), entry.getValue(), reportTime, payload, metadata));
        }
        return points;
    }

    /**
     * 解析单个时间点上的监测值。
     *
     * <p>核心转换逻辑：支持三种原始 value 类型转换为标准时序点：
     * <ol>
     *   <li>数值类型（Number）→ 单属性单点，attrCode 使用默认值 "value"</li>
     *   <li>JSON 对象 → 展开为多属性点，每个键对应一个属性编码</li>
     *   <li>字符串：
     *     <ul>
     *       <li>不含逗号 → 作为单一数值解析</li>
     *       <li>含逗号 → 按顺序与传感器属性列表对齐（按 id 排序）</li>
     *     </ul>
     *   </li>
     * </ol>
     * </p>
     *
     * @param topic      监测主题信息
     * @param timeValue  数据时间原始值（支持时间戳数字或 ISO-8601 字符串）
     * @param rawValue   指标值原始对象
     * @param reportTime 上报时间（时间值解析失败时的兜底）
     * @param payload    原始字符串报文（用于生成 SHA-256 摘要，便于后续去重或追溯）
     * @param metadata   传感器元数据
     * @return 标准化时序点集合（对于不支持的 value 类型抛出异常）
     * @throws ServiceException value 类型不支持时抛出
     */
    private List<StandardMeasurementPoint> parseDataPoint(MonitorTopic topic,
                                                          Object timeValue,
                                                          Object rawValue,
                                                          long reportTime,
                                                          String payload,
                                                          SensorMetadata metadata) {
        // 解析数据时间：优先使用 timeValue，若解析失败则回退到 reportTime
        long dataTime = resolveTimestamp(timeValue, reportTime);
        int quality = 0; // 质量码默认 0，表示数据有效
        long receiveTime = Instant.now().toEpochMilli();
        String payloadHash = buildHash(payload); // 对原始报文做 SHA-256 摘要，用于去重或问题追溯
        List<StandardMeasurementPoint> points = new ArrayList<>();

        if (rawValue instanceof Number number) {
            // 单值数值：attrCode 默认为 "value"
            SensorAttribute attribute = resolveScalarAttribute(metadata);
            points.add(buildPoint(topic, metadata, attribute.getAttrCode(), attribute.getAttrName(), attribute.getUnit(),
                    dataTime, number.doubleValue(), quality, reportTime, receiveTime, payloadHash));
            return points;
        }
        if (rawValue instanceof JSONObject valueObject) {
            // 多值对象：每个键值对对应一个属性编码
            for (Map.Entry<String, Object> entry : valueObject.entrySet()) {
                Double normalized = toDouble(entry.getValue());
                if (normalized == null) {
                    continue;
                }
                SensorAttribute attribute = resolveNamedAttribute(metadata, entry.getKey());
                points.add(buildPoint(topic, metadata, attribute.getAttrCode(), attribute.getAttrName(), attribute.getUnit(),
                        dataTime, normalized, quality, reportTime, receiveTime, payloadHash));
            }
            return points;
        }
        if (rawValue instanceof String stringValue) {
            if (!stringValue.contains(",")) {
                // 单值字符串：尝试转为数值
                SensorAttribute attribute = resolveScalarAttribute(metadata);
                points.add(buildPoint(topic, metadata, attribute.getAttrCode(), attribute.getAttrName(), attribute.getUnit(),
                        dataTime, toDouble(stringValue), quality, reportTime, receiveTime, payloadHash));
                return points;
            }
            // CSV 格式字符串：按顺序与属性列表对齐（属性已在元数据加载时按 id 升序排列）
            List<SensorAttribute> attributes = metadata.attributes();
            if (attributes.isEmpty()) {
                throw new ServiceException("传感器未配置属性，无法解析 CSV 多值数据。请为传感器添加属性定义，或使用单值格式");
            }
            String[] values = stringValue.split(",");
            for (int i = 0; i < values.length && i < attributes.size(); i++) {
                Double normalized = toDouble(values[i]);
                if (normalized == null) {
                    continue;
                }
                SensorAttribute attribute = attributes.get(i);
                points.add(buildPoint(topic, metadata, attribute.getAttrCode(), attribute.getAttrName(), attribute.getUnit(),
                        dataTime, normalized, quality, reportTime, receiveTime, payloadHash));
            }
            return points;
        }
        throw new ServiceException("通用报文 value 类型不支持");
    }

    /**
     * 构建统一标准化时序点。
     * 将解析后的数据组装为 {@link StandardMeasurementPoint}，包含设备标识、传感器标识、属性信息、时间、质量码及报文摘要。
     *
     * @param topic       监测主题信息
     * @param metadata    传感器元数据
     * @param attrCode    属性编码
     * @param attrName    属性名称
     * @param unit        单位
     * @param dataTime    数据时间（毫秒时间戳）
     * @param value       指标值
     * @param quality     质量码（0 = 有效）
     * @param reportTime  上报时间（设备侧生成，毫秒时间戳）
     * @param receiveTime 接收时间（Broker 接收时刻，毫秒时间戳）
     * @param payloadHash 报文 SHA-256 摘要（用于去重或问题追溯）
     * @return 标准化时序点
     */
    private StandardMeasurementPoint buildPoint(MonitorTopic topic,
                                                SensorMetadata metadata,
                                                String attrCode,
                                                String attrName,
                                                String unit,
                                                long dataTime,
                                                Double value,
                                                int quality,
                                                long reportTime,
                                                long receiveTime,
                                                String payloadHash) {
        return StandardMeasurementPoint.builder()
                .deviceId(metadata.deviceId())
                .sensorNo(topic.sensorNo())
                .sensorId(metadata.sensorId())
                .attrCode(attrCode)
                .attrName(attrName)
                .unit(unit)
                .dataTime(dataTime)
                .value(value)
                .quality(quality)
                .reportTime(reportTime)
                .receiveTime(receiveTime)
                .sourceType(topic.sourceType())
                .payloadHash(payloadHash)
                .build();
    }

    /**
     * 解析默认单值属性定义。
     * 当 value 为数值类型且无明确属性编码时，使用默认 attrCode = "value" 查找对应属性定义。
     *
     * @param metadata 传感器元数据
     * @return 单值属性定义（若查找失败则回退到首个属性或兜底属性）
     */
    private SensorAttribute resolveScalarAttribute(SensorMetadata metadata) {
        return resolveNamedAttribute(metadata, DEFAULT_ATTR_CODE);
    }

    /**
     * 按属性编码查找测点属性定义。
     * 优先精确匹配指定编码；若未找到且属性列表非空则回退到首个属性；
     * 若属性列表也为空，则构建一个以 candidate 为编码和名称的兜底属性。
     *
     * @param metadata  传感器元数据
     * @param candidate 候选属性编码
     * @return 匹配到的属性定义；若不存在则回退到首个属性或兜底属性
     */
    private SensorAttribute resolveNamedAttribute(SensorMetadata metadata, String candidate) {
        return metadata.attributes().stream()
                .filter(attribute -> Objects.equals(attribute.getAttrCode(), candidate))
                .findFirst()
                .orElseGet(() -> metadata.attributes().isEmpty()
                        ? buildFallbackAttribute(candidate)
                        : metadata.attributes().get(0));
    }

    /**
     * 构建兜底属性定义。
     * 当传感器元数据中没有任何属性定义时使用，保证解析流程不中断。
     *
     * @param candidate 候选属性编码
     * @return 兜底属性定义（attrCode 和 attrName 均使用 candidate，unit 为 null）
     */
    private SensorAttribute buildFallbackAttribute(String candidate) {
        return SensorAttribute.builder()
                .attrCode(candidate)
                .attrName(candidate)
                .unit(null)
                .build();
    }

    /**
     * 判断对象是否为时间戳映射结构。
     * 检查 JSON 对象的所有键是否都能解析为时间戳，用于区分普通键值对象与历史数据映射。
     *
     * @param object JSON 对象
     * @return 全部键均可解析为时间戳时返回 {@code true}
     */
    private boolean looksLikeHistoryMap(JSONObject object) {
        return object.keySet().stream().allMatch(this::looksLikeTimestamp);
    }

    /**
     * 判断测点键是否与目标传感器匹配。
     * 匹配规则：测点键以 "_" 结尾，后接传感器编号。
     * 例如 sensorNo="01"，measurementKey="temperature_01" → 匹配
     *
     * @param sensorNo       传感器编号
     * @param measurementKey 报文中的测点键
     * @return 匹配时返回 {@code true}
     */
    private boolean measurementMatchesSensor(String sensorNo, String measurementKey) {
        return measurementKey != null && measurementKey.endsWith("_" + sensorNo);
    }

    /**
     * 判断字符串是否可解析为时间戳。
     * 尝试将字符串传给 {@link #resolveTimestamp}，若不抛异常则视为时间戳。
     *
     * @param value 待检测字符串
     * @return 可解析时返回 {@code true}
     */
    private boolean looksLikeTimestamp(String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        try {
            resolveTimestamp(value, -1L);
            return true;
        } catch (RuntimeException e) {
            // resolveTimestamp 仅对非数字非 ISO-8601 格式的字符串抛出 DateTimeParseException，
            // 即无法解析为时间戳的值，视为非时间戳键。
            return false;
        }
    }

    /**
     * 解析时间字段。
     * 支持三种格式：null（回退默认值）、纯数字时间戳、ISO-8601 字符串。
     *
     * <p>优先级：
     * <ol>
     *   <li>rawValue 为 null → 返回 defaultValue</li>
     *   <li>rawValue 为 Number → 直接转为 long（视为毫秒时间戳）</li>
     *   <li>rawValue 为纯数字字符串 → 解析为 long</li>
     *   <li>其余情况 → 视为 ISO-8601 时间字符串，解析为毫秒时间戳</li>
     * </ol>
     * </p>
     *
     * @param rawValue     原始时间值
     * @param defaultValue 默认时间值（rawValue 为 null 时使用）
     * @return 毫秒时间戳
     * @throws java.time.format.DateTimeParseException 当时间字符串格式非法时抛出
     */
    private long resolveTimestamp(Object rawValue, long defaultValue) {
        if (rawValue == null) {
            return defaultValue;
        }
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(rawValue);
        if (text.chars().allMatch(Character::isDigit)) {
            return Long.parseLong(text);
        }
        return Instant.parse(text).toEpochMilli();
    }

    /**
     * 将原始值转换为双精度数值。
     * 支持 Number 类型直接转换，以及可解析为数值的字符串。
     * 空字符串返回 null，不会抛出异常。
     *
     * @param rawValue 原始值
     * @return 数值结果；空值或解析失败时返回 {@code null}
     * @throws NumberFormatException 当字符串无法转换为数字时抛出（由调用方决定是否捕获）
     */
    private Double toDouble(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        String text = String.valueOf(rawValue).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Double.parseDouble(text);
    }

    /**
     * 为原始报文生成 SHA-256 摘要。
     * 用于标识同一批次数据，实现去重和问题追溯。每个时序点均携带此摘要。
     *
     * @param payload 原始字符串报文
     * @return 报文摘要（64 位十六进制字符串）
     * @throws ServiceException 当摘要算法不可用时抛出
     */
    private String buildHash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("生成报文摘要失败").setDetailMessage(e.getMessage());
        }
    }
}
