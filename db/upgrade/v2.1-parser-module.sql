-- ============================================================
-- zwei-iot-parser 模块数据库迁移
-- 版本: v2.1
-- 描述: 新建数据解析策略管理表 + 运行日志表 + 预置策略
-- ============================================================

-- 1. 解析策略表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `source_type` varchar(50) NOT NULL COMMENT '协议标识(sys/gb/自定义)',
  `description` text COMMENT '描述',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 0-停用 1-启用',
  `app_scope` varchar(20) NOT NULL DEFAULT 'global' COMMENT '应用范围 global/vendor/device',
  `script_code` mediumtext NOT NULL COMMENT 'Groovy解析脚本',
  `is_preset` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否预置策略 0-否 1-是',
  `last_run_time` datetime DEFAULT NULL COMMENT '最近运行时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `update_by` bigint DEFAULT NULL COMMENT '更新人ID',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '删除标记 0-否 1-是',
  PRIMARY KEY (`id`),
  KEY `idx_source_type` (`source_type`),
  KEY `idx_status` (`status`),
  KEY `idx_app_scope` (`app_scope`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据解析策略表';

-- 2. 策略-厂商关联表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy_vendor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `vendor_id` bigint NOT NULL COMMENT '厂商ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_vendor` (`strategy_id`, `vendor_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_vendor_id` (`vendor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-厂商关联表';

-- 3. 策略-设备关联表
CREATE TABLE IF NOT EXISTS `iot_data_parse_strategy_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_device` (`strategy_id`, `device_id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_device_id` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析策略-设备关联表';

-- 4. 运行日志表
CREATE TABLE IF NOT EXISTS `iot_data_parse_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` bigint NOT NULL COMMENT '策略ID',
  `log_level` varchar(20) NOT NULL COMMENT '日志级别 INFO/WARN/ERROR',
  `message` text NOT NULL COMMENT '日志消息',
  `data` text COMMENT '关联数据(JSON)',
  `topic` varchar(200) DEFAULT NULL COMMENT '消息主题',
  `device_code` varchar(100) DEFAULT NULL COMMENT '设备编码',
  `parse_result` text COMMENT '解析结果(JSON)',
  `execution_time` int DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `error_stack` text COMMENT '错误堆栈',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`),
  KEY `idx_log_level` (`log_level`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_device_code` (`device_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='解析运行日志表';

-- 5. 预置策略: 系统自定义协议 (sys)
INSERT INTO `iot_data_parse_strategy` (`name`, `source_type`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
(
  '系统协议解析',
  'sys',
  '系统自定义JSON协议解析策略，支持标准格式(version+data)和传统格式(嵌套deviceId键)',
  1,
  'global',
  'import groovy.json.JsonSlurper


Map<String, Object> parse(String topic, byte[] messageBytes) {
    String payload = new String(messageBytes, "UTF-8")
    def json = new JsonSlurper().parseText(payload) as Map<String, Object>
    def result = [
        sensorCode: (json.getOrDefault("sensorNo", "") ?: "").toString(),
        dataTime: 0L,
        properties: []
    ]

    if ((json.containsKey("version") || json.containsKey("data")) && json.get("data") != null) {
        def ts = json.get("timestamp")
        result.put("dataTime", ts != null ? resolveTimestamp(ts) : builtin.currentTimeMillis())
        parseStandardData(json.get("data"), result)
    } else {
        result.put("dataTime", builtin.currentTimeMillis())
        parseLegacyData(json, result)
    }
    result.put("sensorCode", result["sensorCode"] ?: "1")
    return result
}


private void parseStandardData(Object data, Map<String, Object> result) {
    if (data instanceof List) {
        List items = (List) data
        List<Map<String, Object>> props = []
        for (item in items) {
            if (item instanceof Map) {
                Map m = (Map) item
                if (m.containsKey("time") || m.containsKey("value")) {
                    def ts = m.get("timestamp")
                    if (ts != null) result.put("dataTime", resolveTimestamp(ts))
                    parseSingleDataPoint(m, props)
                } else {
                    props.addAll(objectToProperties(m))
                }
            }
        }
        result.put("properties", props)
    } else if (data instanceof Map) {
        Map dataMap = (Map) data
        if (looksLikeHistoryMap(dataMap)) {
            List<Map<String, Object>> props = []
            for (entry in dataMap) {
                String key = entry.key.toString()
                Map<String, Object> p = toProperty(key, entry.value)
                p.put("dataTime", parseTimestampString(key))
                props.add(p)
            }
            result.put("properties", props)
        } else if (dataMap.containsKey("time") || dataMap.containsKey("value")) {
            List<Map<String, Object>> props = []
            parseSingleDataPoint(dataMap, props)
            result.put("properties", props)
        } else {
            result.put("properties", objectToProperties(dataMap))
        }
    }
}


private void parseSingleDataPoint(Map m, List<Map<String, Object>> props) {
    Object rawValue = m.get("value")
    if (rawValue instanceof Number) {
        props.add(toProperty("value", rawValue))
    } else if (rawValue instanceof String) {
        String s = (String) rawValue
        if (s.contains(",")) {
            def parts = s.split(",")
            for (int i = 0; i < parts.length; i++) {
                props.add(toProperty("value_" + i, parts[i].trim()))
            }
        } else {
            props.add(toProperty("value", s))
        }
    } else if (rawValue instanceof Map) {
        props.addAll(objectToProperties((Map) rawValue))
    }
}


private void parseLegacyData(Map json, Map<String, Object> result) {
    List<Map<String, Object>> props = []
    for (topKey in json.keySet()) {
        def topValue = json.get(topKey)
        if (topValue instanceof Map) {
            Map topMap = (Map) topValue
            for (measKey in topMap.keySet()) {
                if (measKey.toString().endsWith("_" + result["sensorCode"])) {
                    def measValue = topMap.get(measKey)
                    if (measValue instanceof Map) {
                        Map measMap = (Map) measValue
                        for (tsKey in measMap.keySet()) {
                            props.add(toProperty("value", measMap.get(tsKey)))
                        }
                    } else {
                        props.add(toProperty("value", measValue))
                    }
                }
            }
        }
    }
    result.put("properties", props)
}


private List<Map<String, Object>> objectToProperties(Map m) {
    List<Map<String, Object>> props = []
    for (entry in m) {
        String key = entry.key.toString()
        if (key != "time" && key != "timestamp" && key != "version" && key != "sensorNo") {
            props.add(toProperty(key, entry.value))
        }
    }
    return props
}


private Map<String, Object> toProperty(String identifier, Object value) {
    return [
        identifier: identifier,
        value: toDouble(value),
        quality: 0
    ]
}


private Double toDouble(Object v) {
    if (v == null) return null
    if (v instanceof Number) return ((Number) v).doubleValue()
    try { return Double.parseDouble(v.toString().trim()) } catch (Exception ignored) { return null }
}


private long resolveTimestamp(Object ts) {
    if (ts == null) return builtin.currentTimeMillis()
    if (ts instanceof Number) return ((Number) ts).longValue()
    try {
        String s = ts.toString().trim()
        if (s =~ /^\d{13}$/) return Long.parseLong(s)
        if (s =~ /^\d{10}$/) return Long.parseLong(s) * 1000L
        return builtin.currentTimeMillis()
    } catch (Exception ignored) {
        return builtin.currentTimeMillis()
    }
}


private long parseTimestampString(String s) {
    try {
        if (s =~ /^\d{13}$/) return Long.parseLong(s)
        if (s =~ /^\d{10}$/) return Long.parseLong(s) * 1000L
        return builtin.currentTimeMillis()
    } catch (Exception ignored) {
        return builtin.currentTimeMillis()
    }
}


private boolean looksLikeHistoryMap(Map m) {
    for (key in m.keySet()) {
        if (!(key =~ /^\d+$/)) return false
    }
    return m.size() > 0
}


private void setResult(Map result, String sensorCode, long dataTime, List props) {
    result.put("sensorCode", sensorCode)
    result.put("dataTime", dataTime)
    result.put("properties", props)
}

return [sensorCode: "1", dataTime: 0L, properties: []]',
  1
);

-- 6. 预置策略: 国标协议 (gb)
INSERT INTO `iot_data_parse_strategy` (`name`, `source_type`, `description`, `status`, `app_scope`, `script_code`, `is_preset`) VALUES
(
  '国标协议解析',
  'gb',
  '国标水文/地质灾害监测数据协议，基于hex字节流解析。参考: http://ghiot.cigem.cn:8080/doc/overview/overview.html',
  1,
  'global',
  '

Map<String, Object> parse(String topic, byte[] messageBytes) {
    String hexPayload = new String(messageBytes, "UTF-8")
    byte[] bytes = builtin.hexDecode(hexPayload)
    def result = [
        sensorCode: "1",
        dataTime: builtin.currentTimeMillis(),
        properties: []
    ]
    int offset = 4
    String deviceCode = builtin.readAscii(bytes, offset, 16).trim()
    offset += 16
    long dataTime = builtin.readBcdTimestamp(bytes, offset)
    offset += 8
    while (offset + 4 <= bytes.length) {
        int attrCode = builtin.readUInt16(bytes, offset)
        offset += 2
        int valLen = builtin.readUInt8(bytes, offset) as int
        offset += 1
        double value = 0.0
        switch (valLen) {
            case 2: value = (double) builtin.readInt16(bytes, offset); break
            case 4: value = (double) builtin.readFloat(bytes, offset); break
            case 8: value = builtin.readDouble(bytes, offset); break
            default: value = 0.0; break
        }
        offset += valLen
        int quality = builtin.readUInt8(bytes, offset) as int
        offset += 1
        (result["properties"] as List).add([
            identifier: "attr_" + attrCode,
            value: value,
            quality: quality
        ])
    }
    for (prop in result["properties"]) {
        if (prop.value == null || Double.isNaN((double) prop.value) || Double.isInfinite((double) prop.value)) {
            prop.value = null
            prop.quality = 9
        }
    }
    return result
}',
  1
);
