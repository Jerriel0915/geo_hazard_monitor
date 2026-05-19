package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.cache.config.CacheWarmupTaskRegistry;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MonitorTypeServiceImpl单元测试
 * <p>
 * 测试监测类型服务实现类的各项功能，包括：
 * - 分页查询
 * - 全量查询
 * - 按ID查询
 * - 按编码查询
 * - 新增
 * - 修改
 * - 删除（单个和批量）
 * - 编码唯一性校验
 *
 * @author zwei
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MonitorTypeServiceImpl Tests")
class MonitorTypeServiceImplTest {

    @Mock
    private MonitorTypeMapper monitorTypeMapper;

    @Mock
    private IotCacheService cacheService;

    @Mock
    private CacheWarmupTaskRegistry registry;

    private MonitorTypeServiceImpl monitorTypeService;

    @BeforeEach
    void setUp() {
        monitorTypeService = new MonitorTypeServiceImpl(monitorTypeMapper, cacheService, registry);
    }

    @Nested
    @DisplayName("selectMonitorTypePage")
    class SelectMonitorTypePage {

        @Test
        @DisplayName("returns list from mapper")
        void returnsListFromMapper() {
            MonitorType expected = new MonitorType();
            expected.setId(1L);
            expected.setCode("MT001");
            expected.setName("雨量监测");
            when(monitorTypeMapper.selectMonitorTypeList(expected)).thenReturn(List.of(expected));

            List<MonitorType> result = monitorTypeService.selectMonitorTypePage(expected, 1, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("MT001");
            verify(monitorTypeMapper).selectMonitorTypeList(expected);
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() {
            MonitorType query = new MonitorType();
            when(monitorTypeMapper.selectMonitorTypeList(query)).thenReturn(List.of());

            List<MonitorType> result = monitorTypeService.selectMonitorTypePage(query, 1, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectMonitorTypeAll")
    class SelectMonitorTypeAll {

        @Test
        @DisplayName("returns all monitor types from mapper")
        void returnsAllMonitorTypesFromMapper() {
            MonitorType mt1 = new MonitorType();
            mt1.setId(1L);
            mt1.setCode("MT001");
            MonitorType mt2 = new MonitorType();
            mt2.setId(2L);
            mt2.setCode("MT002");
            when(monitorTypeMapper.selectMonitorTypeAll()).thenReturn(Arrays.asList(mt1, mt2));

            List<MonitorType> result = monitorTypeService.selectMonitorTypeAll();

            assertThat(result).hasSize(2);
            verify(monitorTypeMapper).selectMonitorTypeAll();
        }
    }

    @Nested
    @DisplayName("selectMonitorTypeById")
    class SelectMonitorTypeById {

        @Test
        @DisplayName("returns monitor type when exists")
        void returnsMonitorTypeWhenExists() {
            MonitorType expected = new MonitorType();
            expected.setId(100L);
            expected.setCode("MT100");
            when(monitorTypeMapper.selectMonitorTypeById(100L)).thenReturn(expected);

            MonitorType result = monitorTypeService.selectMonitorTypeById(100L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCode()).isEqualTo("MT100");
        }

        @Test
        @DisplayName("returns null when not found")
        void returnsNullWhenNotFound() {
            when(monitorTypeMapper.selectMonitorTypeById(999L)).thenReturn(null);

            MonitorType result = monitorTypeService.selectMonitorTypeById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("selectMonitorTypeByCode")
    class SelectMonitorTypeByCode {

        @Test
        @DisplayName("returns monitor type when code exists")
        void returnsMonitorTypeWhenCodeExists() {
            MonitorType expected = new MonitorType();
            expected.setId(1L);
            expected.setCode("RAIN001");
            when(monitorTypeMapper.selectMonitorTypeByCode("RAIN001")).thenReturn(expected);

            MonitorType result = monitorTypeService.selectMonitorTypeByCode("RAIN001");

            assertThat(result.getCode()).isEqualTo("RAIN001");
            verify(monitorTypeMapper).selectMonitorTypeByCode("RAIN001");
        }

        @Test
        @DisplayName("returns null when code not found")
        void returnsNullWhenCodeNotFound() {
            when(monitorTypeMapper.selectMonitorTypeByCode("NOTFOUND")).thenReturn(null);

            MonitorType result = monitorTypeService.selectMonitorTypeByCode("NOTFOUND");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("insertMonitorType")
    class InsertMonitorType {

        @Test
        @DisplayName("inserts successfully")
        void insertsSuccessfully() {
            MonitorType monitorType = new MonitorType();
            monitorType.setCode("MT001");
            monitorType.setName("雨量监测");
            when(monitorTypeMapper.insertMonitorType(monitorType)).thenReturn(1);

            int result = monitorTypeService.insertMonitorType(monitorType);

            assertThat(result).isEqualTo(1);
            verify(monitorTypeMapper).insertMonitorType(monitorType);
        }

        @Test
        @DisplayName("returns 0 when insert fails")
        void returnsZeroWhenInsertFails() {
            MonitorType monitorType = new MonitorType();
            monitorType.setCode("MT002");
            when(monitorTypeMapper.insertMonitorType(monitorType)).thenReturn(0);

            int result = monitorTypeService.insertMonitorType(monitorType);

            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateMonitorType")
    class UpdateMonitorType {

        @Test
        @DisplayName("delegates to mapper")
        void delegatesToMapper() {
            MonitorType monitorType = new MonitorType();
            monitorType.setId(1L);
            monitorType.setName("Updated Name");
            when(monitorTypeMapper.updateMonitorType(monitorType)).thenReturn(1);

            int result = monitorTypeService.updateMonitorType(monitorType);

            assertThat(result).isEqualTo(1);
            verify(monitorTypeMapper).updateMonitorType(monitorType);
        }
    }

    @Nested
    @DisplayName("deleteMonitorTypeById")
    class DeleteMonitorTypeById {

        @Test
        @DisplayName("delegates to mapper")
        void delegatesToMapper() {
            when(monitorTypeMapper.deleteMonitorTypeById(1L)).thenReturn(1);

            int result = monitorTypeService.deleteMonitorTypeById(1L);

            assertThat(result).isEqualTo(1);
            verify(monitorTypeMapper).deleteMonitorTypeById(1L);
        }
    }

    @Nested
    @DisplayName("deleteMonitorTypeByIds")
    class DeleteMonitorTypeByIds {

        @Test
        @DisplayName("delegates to mapper with array")
        void delegatesToMapperWithArray() {
            Long[] ids = {1L, 2L, 3L};
            when(monitorTypeMapper.deleteMonitorTypeByIds(ids)).thenReturn(3);

            int result = monitorTypeService.deleteMonitorTypeByIds(ids);

            assertThat(result).isEqualTo(3);
            verify(monitorTypeMapper).deleteMonitorTypeByIds(ids);
        }

        @Test
        @DisplayName("returns 0 when ids is empty")
        void returnsZeroWhenIdsEmpty() {
            Long[] ids = {};
            when(monitorTypeMapper.deleteMonitorTypeByIds(ids)).thenReturn(0);

            int result = monitorTypeService.deleteMonitorTypeByIds(ids);

            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("checkMonitorTypeCodeUnique")
    class CheckMonitorTypeCodeUnique {

        @Test
        @DisplayName("returns true when code is new (no existing record)")
        void returnsTrueWhenCodeIsNew() {
            MonitorType monitorType = new MonitorType();
            monitorType.setCode("NEW001");
            when(monitorTypeMapper.checkMonitorTypeCodeUnique("NEW001", 0L)).thenReturn(null);

            boolean result = monitorTypeService.checkMonitorTypeCodeUnique(monitorType);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns true when code belongs to same monitor type (id matches)")
        void returnsTrueWhenCodeBelongsToSameMonitorType() {
            // When id matches, SQL's "id != #{id}" excludes that record
            // So no duplicate found, returns null -> service returns true (unique)
            MonitorType monitorType = new MonitorType();
            monitorType.setId(1L);
            monitorType.setCode("MT001");
            MonitorType existing = new MonitorType();
            existing.setId(1L);
            existing.setCode("MT001");
            when(monitorTypeMapper.checkMonitorTypeCodeUnique("MT001", 1L)).thenReturn(null);

            boolean result = monitorTypeService.checkMonitorTypeCodeUnique(monitorType);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code belongs to different monitor type")
        void returnsFalseWhenCodeBelongsToDifferentMonitorType() {
            MonitorType monitorType = new MonitorType();
            monitorType.setId(1L);
            monitorType.setCode("MT001");
            MonitorType existing = new MonitorType();
            existing.setId(2L);
            existing.setCode("MT001");
            when(monitorTypeMapper.checkMonitorTypeCodeUnique("MT001", 1L)).thenReturn(existing);

            boolean result = monitorTypeService.checkMonitorTypeCodeUnique(monitorType);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true when monitor type id is null (new insert)")
        void returnsTrueWhenMonitorTypeIdIsNull() {
            MonitorType monitorType = new MonitorType();
            monitorType.setId(null);
            monitorType.setCode("MT001");
            when(monitorTypeMapper.checkMonitorTypeCodeUnique("MT001", 0L)).thenReturn(null);

            boolean result = monitorTypeService.checkMonitorTypeCodeUnique(monitorType);

            assertThat(result).isTrue();
        }
    }
}