package com.zwei.iot.alarm.service.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptLoggerTest {

    @Test
    void collectLogs_infoWarnError() {
        ScriptLogger logger = new ScriptLogger(1L);
        logger.info("开始检查");
        logger.warn("雨量异常");
        logger.error("设备离线");

        String json = logger.toJson();
        assertNotNull(json);
        JSONArray arr = JSON.parseArray(json);
        assertEquals(3, arr.size());
        assertEquals("INFO", arr.getJSONObject(0).getString("level"));
        assertEquals("开始检查", arr.getJSONObject(0).getString("msg"));
        assertEquals("WARN", arr.getJSONObject(1).getString("level"));
        assertEquals("ERROR", arr.getJSONObject(2).getString("level"));
    }

    @Test
    void toJson_noEntries_returnsNull() {
        ScriptLogger logger = new ScriptLogger(1L);
        assertNull(logger.toJson());
    }

    @Test
    void ts_isRelative() throws InterruptedException {
        ScriptLogger logger = new ScriptLogger(1L);
        Thread.sleep(10);
        logger.info("after delay");
        String json = logger.toJson();
        JSONArray arr = JSON.parseArray(json);
        long ts = arr.getJSONObject(0).getLong("ts");
        assertTrue(ts >= 10, "ts should be >= 10ms, got " + ts);
    }
}
