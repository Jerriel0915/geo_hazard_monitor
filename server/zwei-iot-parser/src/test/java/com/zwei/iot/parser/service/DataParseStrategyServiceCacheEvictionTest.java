package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 验证 DataParseStrategyService 在 update/delete/toggleStatus/copy 后
 * 调用 {@link GroovyScriptEngine#evictCache(Long)} 清除脚本编译缓存 (B1 修复)。
 */
@DisplayName("DataParseStrategyService — 缓存淘汰")
class DataParseStrategyServiceCacheEvictionTest {

    private DataParseStrategyService service;
    private DataParseStrategyMapper strategyMapper;
    private DataParseStrategyVendorMapper vendorMapper;
    private DataParseStrategyDeviceMapper deviceMapper;
    private GroovyScriptEngine scriptEngine;
    private ITopicPatternService topicPatternService;

    @BeforeEach
    void setUp() {
        service = new DataParseStrategyService();
        strategyMapper = mock(DataParseStrategyMapper.class);
        vendorMapper = mock(DataParseStrategyVendorMapper.class);
        deviceMapper = mock(DataParseStrategyDeviceMapper.class);
        scriptEngine = mock(GroovyScriptEngine.class);
        topicPatternService = mock(ITopicPatternService.class);
        ReflectionTestUtils.setField(service, "strategyMapper", strategyMapper);
        ReflectionTestUtils.setField(service, "vendorMapper", vendorMapper);
        ReflectionTestUtils.setField(service, "strategyDeviceMapper", deviceMapper);
        ReflectionTestUtils.setField(service, "scriptEngine", scriptEngine);
        ReflectionTestUtils.setField(service, "topicPatternService", topicPatternService);
    }

    @Test
    @DisplayName("update 后淘汰缓存")
    void updateEvictsCache() {
        DataParseStrategy existing = new DataParseStrategy();
        existing.setId(7L);
        existing.setName("old");
        when(strategyMapper.selectById(7L)).thenReturn(existing);
        when(strategyMapper.checkNameUnique(any(), any())).thenReturn(null);

        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        dto.setId(7L);
        dto.setName("new");
        dto.setSourceType("sys");
        dto.setScriptCode(validScript());
        dto.setAppScope("global");
        dto.setStatus(1);

        service.update(dto);

        verify(scriptEngine).evictCache(7L);
    }

    @Test
    @DisplayName("delete 后淘汰缓存")
    void deleteEvictsCache() {
        DataParseStrategy existing = new DataParseStrategy();
        existing.setId(8L);
        when(strategyMapper.selectById(8L)).thenReturn(existing);

        service.delete(8L);

        verify(strategyMapper).deleteById(8L);
        verify(scriptEngine).evictCache(8L);
    }

    @Test
    @DisplayName("toggleStatus 后淘汰缓存")
    void toggleStatusEvictsCache() {
        DataParseStrategy existing = new DataParseStrategy();
        existing.setId(9L);
        when(strategyMapper.selectById(9L)).thenReturn(existing);

        service.toggleStatus(9L, 0);

        verify(scriptEngine).evictCache(9L);
    }

    @Test
    @DisplayName("update 使用批量插入关联表 (B8)")
    void updateUsesBatchInsertForRelations() {
        DataParseStrategy existing = new DataParseStrategy();
        existing.setId(11L);
        existing.setName("old");
        when(strategyMapper.selectById(11L)).thenReturn(existing);
        when(strategyMapper.checkNameUnique(any(), any())).thenReturn(null);

        DataParseStrategyDTO dto = new DataParseStrategyDTO();
        dto.setId(11L);
        dto.setName("new");
        dto.setSourceType("sys");
        dto.setScriptCode(validScript());
        dto.setAppScope("device");
        dto.setStatus(1);
        dto.setDeviceIds(java.util.List.of(1L, 2L, 3L));

        service.update(dto);

        verify(deviceMapper).batchInsert(eq(11L), eq(java.util.List.of(1L, 2L, 3L)));
        verify(deviceMapper, never()).insert(any());
    }

    private String validScript() {
        return "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    return [sensorCode: \"S\", properties: []]\n" +
            "}";
    }
}
