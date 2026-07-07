package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyVendorMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证 DataParseStrategyService CRUD + 新字段 (serverUrl/topic) + keyword 查询 (B6 修复)。
 */
@DisplayName("DataParseStrategyService — CRUD 与字段")
class DataParseStrategyServiceTest {

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

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("应携带 serverUrl / topic 写入")
        void createCarriesNewFields() {
            when(strategyMapper.checkNameUnique(any(), any())).thenReturn(null);

            DataParseStrategyDTO dto = new DataParseStrategyDTO();
            dto.setName("策略A");
            dto.setSourceType("sys");
            dto.setServerUrl("tcp://mqtt:1883");
            dto.setTopic("sys/v1/DEV/S1/updata");
            dto.setScriptCode(validScript());
            dto.setAppScope("global");
            dto.setStatus(1);

            service.create(dto);

            verify(strategyMapper).insert(argThat(s ->
                    "tcp://mqtt:1883".equals(s.getServerUrl())
                            && "sys/v1/DEV/S1/updata".equals(s.getTopic())));
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {
        @Test
        @DisplayName("应回填 serverUrl / topic 及设备关联")
        void getByIdPopulatesFieldsAndRelations() {
            DataParseStrategy strategy = new DataParseStrategy();
            strategy.setId(5L);
            strategy.setName("策略B");
            strategy.setAppScope("device");
            strategy.setServerUrl("tcp://mqtt:1883");
            strategy.setTopic("sys/v1/DEV2/S2/updata");
            when(strategyMapper.selectById(5L)).thenReturn(strategy);
            when(deviceMapper.selectDeviceIdsByStrategyId(5L)).thenReturn(List.of(10L, 20L));

            DataParseStrategyDTO dto = service.getById(5L);

            assertThat(dto.getServerUrl()).isEqualTo("tcp://mqtt:1883");
            assertThat(dto.getTopic()).isEqualTo("sys/v1/DEV2/S2/updata");
            assertThat(dto.getDeviceIds()).containsExactly(10L, 20L);
        }
    }

    @Nested
    @DisplayName("listByPage")
    class ListByPage {
        @Test
        @DisplayName("keyword 应传给 mapper（name 或 topic 模糊匹配）")
        void keywordPassedToMapper() {
            when(strategyMapper.selectByCondition(anyString(), any(), any(), any()))
                    .thenReturn(List.of());

            DataParseStrategyQueryDTO query = new DataParseStrategyQueryDTO();
            query.setKeyword("雨量");
            query.setStatus(1);

            service.listByPage(query);

            verify(strategyMapper).selectByCondition(eq("雨量"), eq(null), eq(1), eq(null));
        }

        @Test
        @DisplayName("无 keyword 时兼容旧 name 字段")
        void nameFallbackWhenNoKeyword() {
            when(strategyMapper.selectByCondition(anyString(), any(), any(), any()))
                    .thenReturn(List.of());

            DataParseStrategyQueryDTO query = new DataParseStrategyQueryDTO();
            query.setName("国标");

            service.listByPage(query);

            verify(strategyMapper).selectByCondition(eq("国标"), eq(null), eq(null), eq(null));
        }
    }

    @Nested
    @DisplayName("copy")
    class Copy {
        @Test
        @DisplayName("副本应继承 serverUrl / topic 并标记非预置 + 停用")
        void copyInheritsFieldsAndResetsPreset() {
            DataParseStrategy original = new DataParseStrategy();
            original.setId(3L);
            original.setName("原策略");
            original.setSourceType("gb");
            original.setAppScope("global");
            original.setServerUrl("tcp://mqtt:1883");
            original.setTopic("gb/v1/DEV/1/updata");
            original.setIsPreset(1);
            original.setStatus(1);
            when(strategyMapper.selectById(3L)).thenReturn(original);

            service.copy(3L);

            verify(strategyMapper).insert(argThat(s ->
                    "原策略 (副本)".equals(s.getName())
                            && "tcp://mqtt:1883".equals(s.getServerUrl())
                            && "gb/v1/DEV/1/updata".equals(s.getTopic())
                            && s.getIsPreset() == 0
                            && s.getStatus() == 0));
        }
    }

    private String validScript() {
        return "Map<String, Object> parse(String topic, byte[] messageBytes) {\n" +
            "    return [sensorCode: \"S\", properties: []]\n" +
            "}";
    }
}
