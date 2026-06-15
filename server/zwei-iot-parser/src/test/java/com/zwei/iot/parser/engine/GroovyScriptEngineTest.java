package com.zwei.iot.parser.engine;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.service.DataParseLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GroovyScriptEngine")
class GroovyScriptEngineTest {

    private GroovyScriptEngine engine;
    private DataParseLogService logService;

    @BeforeEach
    void setUp() {
        logService = mock(DataParseLogService.class);
        engine = new GroovyScriptEngine();
        injectField(engine, "builtInFunctions", new BuiltInFunctions());
        injectField(engine, "logService", logService);
    }

    @Nested
    @DisplayName("Sys protocol — standard JSON format")
    class SysProtocolStandard {

        @Test
        @DisplayName("should parse single numeric value")
        void parseSingleValue() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 1L);
            String payload = "{\"version\":\"1.0\",\"sensorNo\":\"S001\",\"timestamp\":1700000000000,\"data\":{\"value\":25.5}}";
            byte[] message = payload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.sensorCode()).isEqualTo("S001");
            assertThat(result.sourceType()).isEqualTo("sys");
            assertThat(result.dataTime()).isEqualTo(1700000000000L);
            assertThat(result.properties()).hasSize(1);
            assertThat(result.properties().get(0).identifier()).isEqualTo("value");
            assertThat(result.properties().get(0).value()).isEqualTo(25.5);
            assertThat(result.properties().get(0).quality()).isEqualTo(0);
        }

        @Test
        @DisplayName("should parse multi-property JSON object")
        void parseMultiProperty() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 2L);
            String payload = "{\"version\":\"1.0\",\"sensorNo\":\"S001\",\"timestamp\":1700000000000,\"data\":{\"rainfall\":12.3,\"temperature\":25.0,\"humidity\":68.5}}";
            byte[] message = payload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.properties()).hasSize(3);
            assertThat(result.properties()).anyMatch(p -> p.identifier().equals("rainfall") && p.value() == 12.3);
            assertThat(result.properties()).anyMatch(p -> p.identifier().equals("temperature") && p.value() == 25.0);
            assertThat(result.properties()).anyMatch(p -> p.identifier().equals("humidity") && p.value() == 68.5);
        }

        @Test
        @DisplayName("should parse JSON array of data points")
        void parseDataArray() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 3L);
            String payload = "{\"version\":\"1.0\",\"sensorNo\":\"S001\",\"timestamp\":1700000000000,\"data\":[{\"code\":\"rainfall\",\"value\":12.3},{\"code\":\"temperature\",\"value\":25.0}]}";
            byte[] message = payload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.properties()).hasSize(2);
        }

        @Test
        @DisplayName("should parse comma-separated CSV values")
        void parseCsvValues() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 4L);
            String payload = "{\"version\":\"1.0\",\"sensorNo\":\"S001\",\"timestamp\":1700000000000,\"data\":{\"value\":\"12.3,25.0,68.5\"}}";
            byte[] message = payload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.properties()).hasSize(3);
            assertThat(result.properties().get(0).identifier()).isEqualTo("value_0");
            assertThat(result.properties().get(0).value()).isEqualTo(12.3);
            assertThat(result.properties().get(1).identifier()).isEqualTo("value_1");
            assertThat(result.properties().get(2).identifier()).isEqualTo("value_2");
            assertThat(result.properties().get(2).value()).isEqualTo(68.5);
        }
    }

    @Nested
    @DisplayName("Sys protocol — legacy JSON format")
    class SysProtocolLegacy {

        @Test
        @DisplayName("should parse legacy nested deviceId format")
        void parseLegacyFormat() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 5L);
            String payload = "{\"DEV001\":{\"measurement_S001\":{\"1700000000000\":42.5}}}";
            byte[] message = payload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.properties()).isNotEmpty();
            assertThat(result.properties().get(0).value()).isEqualTo(42.5);
            assertThat(result.properties().get(0).identifier()).isEqualTo("value");
        }
    }

    @Nested
    @DisplayName("GB protocol — hex byte stream")
    class GbProtocol {

        @Test
        @DisplayName("should parse hex-encoded byte stream")
        void parseHexByteStream() {
            String script = getGbScript();
            DataParseStrategy strategy = strategy("gb", script, 6L);
            // Build GB frame (hex-encoded ASCII → hexDecode → binary):
            //   header(2B) + len(2B) + deviceId(16B) + bcd(7B) + pad(1B) + dataArea(8B) = 36B
            // Script does offset=4 → device(16B) → bcd(7B) → offset+=8 → data loop
            String header = "0000";
            String deviceIdHex = "54455354444556494345303031202020"; // "TESTDEVICE001   " 16B
            String bcdTime = "20260615120000";   // 7B BCD: YY=20,26=2026 MM=06 DD=15 HH=12 mm=00 ss=00
            String bcdPad = "00";               // 1B pad (script advances 8 after 7B read)
            String dataArea = "00010441CC000000"; // attr=1(2B) + len=4(1B) + float=25.5(4B) + quality=0(1B)
            int totalLen = 2 + 2 + 16 + 7 + 1 + dataArea.length() / 2; // 36
            String frameLenHex = String.format("%04X", totalLen);
            String hexPayload = header + frameLenHex + deviceIdHex + bcdTime + bcdPad + dataArea;
            byte[] message = hexPayload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "gb/v1/DEV001/1/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.sourceType()).isEqualTo("gb");
            assertThat(result.sensorCode()).isEqualTo("1");
            assertThat(result.properties()).hasSize(1);
            assertThat(result.properties().get(0).identifier()).isEqualTo("attr_1");
            assertThat(result.properties().get(0).value()).isCloseTo(25.5, org.assertj.core.data.Offset.offset(0.01));
            assertThat(result.properties().get(0).quality()).isEqualTo(0);
        }

        @Test
        @DisplayName("should parse GB frame with multiple attributes")
        void parseGbMultiAttr() {
            String script = getGbScript();
            DataParseStrategy strategy = strategy("gb", script, 7L);
            String header = "0000";
            String deviceIdHex = "44455630303030303030303030323020"; // "DEV000000000002 " 16B
            String bcdTime = "20260615120000";   // 7B BCD: YY=20,26=2026 MM=06 DD=15 HH=12 mm=00 ss=00
            String bcdPad = "00";
            // Two attributes: float 25.5 (quality=0), float -10.0 (quality=0)
            // Each attr: 2B(attrCode) + 1B(valLen) + 4B(value) + 1B(quality) = 8B = 16 hex chars
            String dataArea = "00010441CC000000" + "000204C120000000";
            int totalLen = 2 + 2 + 16 + 7 + 1 + dataArea.length() / 2;
            String frameLenHex = String.format("%04X", totalLen);
            String hexPayload = header + frameLenHex + deviceIdHex + bcdTime + bcdPad + dataArea;
            byte[] message = hexPayload.getBytes(StandardCharsets.UTF_8);

            ParsedMessage result = engine.execute(strategy, "gb/v1/DEV001/2/updata", message);

            assertThat(result).isNotNull();
            assertThat(result.sourceType()).isEqualTo("gb");
            assertThat(result.properties()).hasSize(2);
            assertThat(result.properties().get(0).identifier()).isEqualTo("attr_1");
            assertThat(result.properties().get(0).value()).isCloseTo(25.5, org.assertj.core.data.Offset.offset(0.01));
            assertThat(result.properties().get(1).identifier()).isEqualTo("attr_2");
            assertThat(result.properties().get(1).value()).isCloseTo(-10.0, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should return null on null payload")
        void nullPayloadReturnsNull() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 8L);
            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should handle empty JSON payload")
        void emptyJsonPayload() {
            String script = getSysScript();
            DataParseStrategy strategy = strategy("sys", script, 9L);
            ParsedMessage result = engine.execute(strategy, "sys/v1/DEV001/S001/updata", "{}".getBytes());
            assertThat(result).isNotNull();
            assertThat(result.properties()).isEmpty();
        }

        @Test
        @DisplayName("should return null for unsafe script with System import")
        void unsafeScriptRejected() {
            String unsafeScript = "import java.lang.System\nMap parse(String t, byte[] m) { System.exit(0); return [:]; }";
            DataParseStrategy strategy = strategy("sys", unsafeScript, 10L);
            engine.evictCache(10L);
            ParsedMessage result = engine.execute(strategy, "sys/v1/D/S/updata", "{}".getBytes());
            assertThat(result).isNull();
        }
    }

    // --- helpers ---

    private DataParseStrategy strategy(String sourceType, String scriptCode, Long id) {
        DataParseStrategy s = new DataParseStrategy();
        s.setId(id);
        s.setSourceType(sourceType);
        s.setScriptCode(scriptCode);
        s.setName("Test-" + id);
        s.setStatus(1);
        return s;
    }

    private void injectField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Sys protocol Groovy script using fastjson2 (available on classpath via zwei-common)
    // Note: must use result.put() not result.xxx = ... because Groovy property setter
    // fails on LinkedHashMap read-only properties.  SensorCode is extracted from the
    // topic parameter when missing from the JSON payload (legacy format).
    private String getSysScript() {
        return "import com.alibaba.fastjson2.JSON\n" +
            "import com.alibaba.fastjson2.JSONObject\n\n" +
            "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    String payload = new String(messageBytes, \"UTF-8\")\n" +
            "    JSONObject json = JSON.parseObject(payload)\n" +
            "    def sensorCode = (json.getOrDefault(\"sensorNo\", \"\") ?: \"\").toString()\n" +
            "    // Fallback: extract sensorCode from topic (sys/v1/{deviceCode}/{sensorCode}/updata)\n" +
            "    if (sensorCode.isEmpty() && topic != null) {\n" +
            "        def parts = topic.split(\"/\")\n" +
            "        if (parts.length >= 4) sensorCode = parts[3]\n" +
            "    }\n" +
            "    def result = [:]\n" +
            "    result.put(\"sensorCode\", sensorCode)\n" +
            "    result.put(\"dataTime\", 0L)\n" +
            "    result.put(\"properties\", [])\n" +
            "    if ((json.containsKey(\"version\") || json.containsKey(\"data\")) && json.get(\"data\") != null) {\n" +
            "        def ts = json.get(\"timestamp\")\n" +
            "        result.put(\"dataTime\", ts != null ? resolveTimestamp(ts) : builtin.currentTimeMillis())\n" +
            "        parseStandardData(json.get(\"data\"), result)\n" +
            "    } else {\n" +
            "        result.put(\"dataTime\", builtin.currentTimeMillis())\n" +
            "        parseLegacyData(json, result)\n" +
            "    }\n" +
            "    result.put(\"sensorCode\", result.get(\"sensorCode\") ?: \"1\")\n" +
            "    return result\n" +
            "}\n\n" +
            "private void parseStandardData(Object data, Map<String, Object> result) {\n" +
            "    if (data instanceof List) {\n" +
            "        List items = (List) data\n" +
            "        List<Map<String, Object>> props = []\n" +
            "        for (item in items) {\n" +
            "            if (item instanceof Map) {\n" +
            "                Map m = (Map) item\n" +
            "                if (m.containsKey(\"time\") || m.containsKey(\"value\")) {\n" +
            "                    def ts = m.get(\"timestamp\")\n" +
            "                    if (ts != null) result.put(\"dataTime\", resolveTimestamp(ts))\n" +
            "                    parseSingleDataPoint(m, props)\n" +
            "                } else {\n" +
            "                    props.addAll(objectToProperties(m))\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        result.put(\"properties\", props)\n" +
            "    } else if (data instanceof Map) {\n" +
            "        Map dataMap = (Map) data\n" +
            "        if (looksLikeHistoryMap(dataMap)) {\n" +
            "            List<Map<String, Object>> props = []\n" +
            "            for (entry in dataMap) {\n" +
            "                String key = entry.key.toString()\n" +
            "                Map<String, Object> p = toProperty(key, entry.value)\n" +
            "                p.put(\"dataTime\", parseTimestampString(key))\n" +
            "                props.add(p)\n" +
            "            }\n" +
            "            result.put(\"properties\", props)\n" +
            "        } else if (dataMap.containsKey(\"time\") || dataMap.containsKey(\"value\")) {\n" +
            "            List<Map<String, Object>> props = []\n" +
            "            parseSingleDataPoint(dataMap, props)\n" +
            "            result.put(\"properties\", props)\n" +
            "        } else {\n" +
            "            result.put(\"properties\", objectToProperties(dataMap))\n" +
            "        }\n" +
            "    }\n" +
            "}\n\n" +
            "private void parseSingleDataPoint(Map m, List<Map<String, Object>> props) {\n" +
            "    Object rawValue = m.get(\"value\")\n" +
            "    if (rawValue instanceof Number) {\n" +
            "        props.add(toProperty(\"value\", rawValue))\n" +
            "    } else if (rawValue instanceof String) {\n" +
            "        String s = (String) rawValue\n" +
            "        if (s.contains(\",\")) {\n" +
            "            def parts = s.split(\",\")\n" +
            "            for (int i = 0; i < parts.length; i++) {\n" +
            "                props.add(toProperty(\"value_\" + i, parts[i].trim()))\n" +
            "            }\n" +
            "        } else {\n" +
            "            props.add(toProperty(\"value\", s))\n" +
            "        }\n" +
            "    } else if (rawValue instanceof Map) {\n" +
            "        props.addAll(objectToProperties((Map) rawValue))\n" +
            "    }\n" +
            "}\n\n" +
            "private void parseLegacyData(Map json, Map<String, Object> result) {\n" +
            "    List<Map<String, Object>> props = []\n" +
            "    for (topKey in json.keySet()) {\n" +
            "        def topValue = json.get(topKey)\n" +
            "        if (topValue instanceof Map) {\n" +
            "            Map topMap = (Map) topValue\n" +
            "            for (measKey in topMap.keySet()) {\n" +
            "                def sensorCode = result.get(\"sensorCode\") ?: \"\"\n" +
            "                if (measKey.toString().endsWith(\"_\" + sensorCode)) {\n" +
            "                    def measValue = topMap.get(measKey)\n" +
            "                    if (measValue instanceof Map) {\n" +
            "                        Map measMap = (Map) measValue\n" +
            "                        for (tsKey in measMap.keySet()) {\n" +
            "                            props.add(toProperty(\"value\", measMap.get(tsKey)))\n" +
            "                        }\n" +
            "                    } else {\n" +
            "                        props.add(toProperty(\"value\", measValue))\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    result.put(\"properties\", props)\n" +
            "}\n\n" +
            "private List<Map<String, Object>> objectToProperties(Map m) {\n" +
            "    List<Map<String, Object>> props = []\n" +
            "    for (entry in m) {\n" +
            "        String key = entry.key.toString()\n" +
            "        if (key != \"time\" && key != \"timestamp\" && key != \"version\" && key != \"sensorNo\") {\n" +
            "            props.add(toProperty(key, entry.value))\n" +
            "        }\n" +
            "    }\n" +
            "    return props\n" +
            "}\n\n" +
            "private Map<String, Object> toProperty(String identifier, Object value) {\n" +
            "    return [\n" +
            "        identifier: identifier,\n" +
            "        value: toDouble(value),\n" +
            "        quality: 0\n" +
            "    ]\n" +
            "}\n\n" +
            "private Double toDouble(Object v) {\n" +
            "    if (v == null) return null\n" +
            "    if (v instanceof Number) return ((Number) v).doubleValue()\n" +
            "    try { return Double.parseDouble(v.toString().trim()) } catch (Exception ignored) { return null }\n" +
            "}\n\n" +
            "private long resolveTimestamp(Object ts) {\n" +
            "    if (ts == null) return builtin.currentTimeMillis()\n" +
            "    if (ts instanceof Number) return ((Number) ts).longValue()\n" +
            "    try {\n" +
            "        String s = ts.toString().trim()\n" +
            "        if (s =~ /^\\d{13}$/) return Long.parseLong(s)\n" +
            "        if (s =~ /^\\d{10}$/) return Long.parseLong(s) * 1000L\n" +
            "        return builtin.currentTimeMillis()\n" +
            "    } catch (Exception ignored) {\n" +
            "        return builtin.currentTimeMillis()\n" +
            "    }\n" +
            "}\n\n" +
            "private long parseTimestampString(String s) {\n" +
            "    try {\n" +
            "        if (s =~ /^\\d{13}$/) return Long.parseLong(s)\n" +
            "        if (s =~ /^\\d{10}$/) return Long.parseLong(s) * 1000L\n" +
            "        return builtin.currentTimeMillis()\n" +
            "    } catch (Exception ignored) {\n" +
            "        return builtin.currentTimeMillis()\n" +
            "    }\n" +
            "}\n\n" +
            "private boolean looksLikeHistoryMap(Map m) {\n" +
            "    for (key in m.keySet()) {\n" +
            "        if (!(key =~ /^\\d+$/)) return false\n" +
            "    }\n" +
            "    return m.size() > 0\n" +
            "}\n" +
            "return [sensorCode: \"1\", dataTime: 0L, properties: []]";
    }

    // GB protocol Groovy script (dynamic, no @CompileStatic to avoid binding variable issues)
    // Uses explicit Map.get()/put() to avoid Groovy dynamic property resolution quirks
    private String getGbScript() {
        return "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    String hexPayload = new String(messageBytes, \"UTF-8\")\n" +
            "    byte[] bytes = builtin.hexDecode(hexPayload)\n" +
            "    def result = [:]\n" +
            "    result.put(\"sensorCode\", \"1\")\n" +
            "    result.put(\"dataTime\", builtin.currentTimeMillis())\n" +
            "    result.put(\"properties\", [])\n" +
            "    int offset = 4\n" +
            "    String deviceCode = builtin.readAscii(bytes, offset, 16).trim()\n" +
            "    offset += 16\n" +
            "    long dataTime = builtin.readBcdTimestamp(bytes, offset)\n" +
            "    offset += 8\n" +
            "    while (offset + 4 <= bytes.length) {\n" +
            "        int attrCode = builtin.readUInt16(bytes, offset)\n" +
            "        offset += 2\n" +
            "        int valLen = builtin.readUInt8(bytes, offset) as int\n" +
            "        offset += 1\n" +
            "        double value = 0.0\n" +
            "        switch (valLen) {\n" +
            "            case 2: value = (double) builtin.readInt16(bytes, offset); break\n" +
            "            case 4: value = (double) builtin.readFloat(bytes, offset); break\n" +
            "            case 8: value = builtin.readDouble(bytes, offset); break\n" +
            "            default: value = 0.0; break\n" +
            "        }\n" +
            "        offset += valLen\n" +
            "        int quality = builtin.readUInt8(bytes, offset) as int\n" +
            "        offset += 1\n" +
            "        ((List) result.get(\"properties\")).add([\n" +
            "            identifier: \"attr_\" + attrCode,\n" +
            "            value: value,\n" +
            "            quality: quality\n" +
            "        ])\n" +
            "    }\n" +
            "    for (prop in ((List) result.get(\"properties\"))) {\n" +
            "        def v = prop.get(\"value\")\n" +
            "        if (v == null || Double.isNaN((double) v) || Double.isInfinite((double) v)) {\n" +
            "            prop.put(\"value\", null)\n" +
            "            prop.put(\"quality\", 9)\n" +
            "        }\n" +
            "    }\n" +
            "    return result\n" +
            "}";
    }
}
