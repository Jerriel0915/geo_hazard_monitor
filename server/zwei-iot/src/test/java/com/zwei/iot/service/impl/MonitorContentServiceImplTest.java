package com.zwei.iot.service.impl;

import com.zwei.iot.config.CacheWarmupTaskRegistry;
import com.zwei.iot.domain.MonitorContent;
import com.zwei.iot.mapper.MonitorContentMapper;
import com.zwei.iot.service.IotCacheService;
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
import static org.mockito.Mockito.*;

/**
 * MonitorContentServiceImpl单元测试
 * <p>
 * 测试监测内容服务实现类的各项功能，包括：
 * - 查询列表
 * - 全量查询（带/不带监测类型过滤）
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
@DisplayName("MonitorContentServiceImpl Tests")
class MonitorContentServiceImplTest {

    @Mock
    private MonitorContentMapper monitorContentMapper;

    @Mock
    private IotCacheService cacheService;

    @Mock
    private CacheWarmupTaskRegistry registry;

    private MonitorContentServiceImpl monitorContentService;

    @BeforeEach
    void setUp() {
        monitorContentService = new MonitorContentServiceImpl(monitorContentMapper, cacheService, registry);
    }

    // ==================== selectMonitorContentList Tests ====================

    @Nested
    @DisplayName("selectMonitorContentList")
    class SelectMonitorContentList {

        @Test
        @DisplayName("returns list from mapper")
        void returnsListFromMapper() {
            MonitorContent expected = new MonitorContent();
            expected.setId(1L);
            expected.setCode("rainfall_hour");
            expected.setName("小时雨量");
            when(monitorContentMapper.selectMonitorContentList(any(MonitorContent.class))).thenReturn(List.of(expected));

            List<MonitorContent> result = monitorContentService.selectMonitorContentList(expected);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("rainfall_hour");
            verify(monitorContentMapper).selectMonitorContentList(expected);
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() {
            MonitorContent query = new MonitorContent();
            when(monitorContentMapper.selectMonitorContentList(any(MonitorContent.class))).thenReturn(List.of());

            List<MonitorContent> result = monitorContentService.selectMonitorContentList(query);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns multiple results")
        void returnsMultipleResults() {
            MonitorContent mc1 = new MonitorContent();
            mc1.setId(1L);
            mc1.setCode("rainfall_hour");
            MonitorContent mc2 = new MonitorContent();
            mc2.setId(2L);
            mc2.setCode("rainfall_day");
            when(monitorContentMapper.selectMonitorContentList(any(MonitorContent.class))).thenReturn(Arrays.asList(mc1, mc2));

            List<MonitorContent> result = monitorContentService.selectMonitorContentList(new MonitorContent());

            assertThat(result).hasSize(2);
        }
    }

    // ==================== selectMonitorContentAll Tests ====================

    @Nested
    @DisplayName("selectMonitorContentAll")
    class SelectMonitorContentAll {

        @Test
        @DisplayName("returns all monitor contents when monitorTypeId is null")
        void returnsAllWhenMonitorTypeIdNull() {
            MonitorContent mc1 = new MonitorContent();
            mc1.setId(1L);
            mc1.setCode("rainfall_hour");
            MonitorContent mc2 = new MonitorContent();
            mc2.setId(2L);
            mc2.setCode("displacement_x");
            when(monitorContentMapper.selectMonitorContentAll(null)).thenReturn(Arrays.asList(mc1, mc2));

            List<MonitorContent> result = monitorContentService.selectMonitorContentAll(null);

            assertThat(result).hasSize(2);
            verify(monitorContentMapper).selectMonitorContentAll(null);
        }

        @Test
        @DisplayName("returns filtered list when monitorTypeId is provided")
        void returnsFilteredListWhenMonitorTypeIdProvided() {
            MonitorContent mc1 = new MonitorContent();
            mc1.setId(1L);
            mc1.setMonitorTypeId(1L);
            mc1.setCode("rainfall_hour");
            when(monitorContentMapper.selectMonitorContentAll(1L)).thenReturn(List.of(mc1));

            List<MonitorContent> result = monitorContentService.selectMonitorContentAll(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMonitorTypeId()).isEqualTo(1L);
            verify(monitorContentMapper).selectMonitorContentAll(1L);
        }

        @Test
        @DisplayName("returns empty list when no matching records")
        void returnsEmptyListWhenNoMatchingRecords() {
            when(monitorContentMapper.selectMonitorContentAll(999L)).thenReturn(List.of());

            List<MonitorContent> result = monitorContentService.selectMonitorContentAll(999L);

            assertThat(result).isEmpty();
        }
    }

    // ==================== selectMonitorContentById Tests ====================

    @Nested
    @DisplayName("selectMonitorContentById")
    class SelectMonitorContentById {

        @Test
        @DisplayName("returns monitor content when exists")
        void returnsMonitorContentWhenExists() {
            MonitorContent expected = new MonitorContent();
            expected.setId(100L);
            expected.setCode("water_level");
            expected.setName("水位");
            when(monitorContentMapper.selectMonitorContentById(100L)).thenReturn(expected);

            MonitorContent result = monitorContentService.selectMonitorContentById(100L);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getCode()).isEqualTo("water_level");
        }

        @Test
        @DisplayName("returns null when not found")
        void returnsNullWhenNotFound() {
            when(monitorContentMapper.selectMonitorContentById(999L)).thenReturn(null);

            MonitorContent result = monitorContentService.selectMonitorContentById(999L);

            assertThat(result).isNull();
        }
    }

    // ==================== selectMonitorContentByCode Tests ====================

    @Nested
    @DisplayName("selectMonitorContentByCode")
    class SelectMonitorContentByCode {

        @Test
        @DisplayName("returns monitor content when code exists")
        void returnsMonitorContentWhenCodeExists() {
            MonitorContent expected = new MonitorContent();
            expected.setId(1L);
            expected.setCode("rainfall_hour");
            when(monitorContentMapper.selectMonitorContentByCode("rainfall_hour")).thenReturn(expected);

            MonitorContent result = monitorContentService.selectMonitorContentByCode("rainfall_hour");

            assertThat(result.getCode()).isEqualTo("rainfall_hour");
            verify(monitorContentMapper).selectMonitorContentByCode("rainfall_hour");
        }

        @Test
        @DisplayName("returns null when code not found")
        void returnsNullWhenCodeNotFound() {
            when(monitorContentMapper.selectMonitorContentByCode("NOTFOUND")).thenReturn(null);

            MonitorContent result = monitorContentService.selectMonitorContentByCode("NOTFOUND");

            assertThat(result).isNull();
        }
    }

    // ==================== insertMonitorContent Tests ====================

    @Nested
    @DisplayName("insertMonitorContent")
    class InsertMonitorContent {

        @Test
        @DisplayName("inserts successfully with all fields")
        void insertsSuccessfullyWithAllFields() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setMonitorTypeId(1L);
            monitorContent.setCode("rainfall_hour");
            monitorContent.setName("小时雨量");
            monitorContent.setUnit("mm");
            monitorContent.setIndicatorType("yl");
            when(monitorContentMapper.insertMonitorContent(monitorContent)).thenReturn(1);

            int result = monitorContentService.insertMonitorContent(monitorContent);

            assertThat(result).isEqualTo(1);
            verify(monitorContentMapper).insertMonitorContent(monitorContent);
        }

        @Test
        @DisplayName("returns 1 when insert succeeds")
        void returnsOneWhenInsertSucceeds() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setCode("TEST001");
            when(monitorContentMapper.insertMonitorContent(monitorContent)).thenReturn(1);

            int result = monitorContentService.insertMonitorContent(monitorContent);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 0 when insert fails")
        void returnsZeroWhenInsertFails() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setCode("FAILED");
            when(monitorContentMapper.insertMonitorContent(monitorContent)).thenReturn(0);

            int result = monitorContentService.insertMonitorContent(monitorContent);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== updateMonitorContent Tests ====================

    @Nested
    @DisplayName("updateMonitorContent")
    class UpdateMonitorContent {

        @Test
        @DisplayName("delegates to mapper with updated fields")
        void delegatesToMapperWithUpdatedFields() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(1L);
            monitorContent.setName("更新后的名称");
            when(monitorContentMapper.updateMonitorContent(monitorContent)).thenReturn(1);

            int result = monitorContentService.updateMonitorContent(monitorContent);

            assertThat(result).isEqualTo(1);
            verify(monitorContentMapper).updateMonitorContent(monitorContent);
        }

        @Test
        @DisplayName("returns 0 when update affects no rows")
        void returnsZeroWhenUpdateAffectsNoRows() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(999L);
            when(monitorContentMapper.updateMonitorContent(monitorContent)).thenReturn(0);

            int result = monitorContentService.updateMonitorContent(monitorContent);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteMonitorContentById Tests ====================

    @Nested
    @DisplayName("deleteMonitorContentById")
    class DeleteMonitorContentById {

        @Test
        @DisplayName("delegates to mapper for single delete")
        void delegatesToMapperForSingleDelete() {
            when(monitorContentMapper.deleteMonitorContentById(1L)).thenReturn(1);

            int result = monitorContentService.deleteMonitorContentById(1L);

            assertThat(result).isEqualTo(1);
            verify(monitorContentMapper).deleteMonitorContentById(1L);
        }

        @Test
        @DisplayName("returns 0 when id does not exist")
        void returnsZeroWhenIdDoesNotExist() {
            when(monitorContentMapper.deleteMonitorContentById(999L)).thenReturn(0);

            int result = monitorContentService.deleteMonitorContentById(999L);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteMonitorContentByIds Tests ====================

    @Nested
    @DisplayName("deleteMonitorContentByIds")
    class DeleteMonitorContentByIds {

        @Test
        @DisplayName("delegates to mapper with array of ids")
        void delegatesToMapperWithArrayOfIds() {
            Long[] ids = {1L, 2L, 3L};
            when(monitorContentMapper.deleteMonitorContentByIds(ids)).thenReturn(3);

            int result = monitorContentService.deleteMonitorContentByIds(ids);

            assertThat(result).isEqualTo(3);
            verify(monitorContentMapper).deleteMonitorContentByIds(ids);
        }

        @Test
        @DisplayName("returns count of deleted rows")
        void returnsCountOfDeletedRows() {
            Long[] ids = {1L, 2L};
            when(monitorContentMapper.deleteMonitorContentByIds(ids)).thenReturn(2);

            int result = monitorContentService.deleteMonitorContentByIds(ids);

            assertThat(result).isEqualTo(2);
        }

        @Test
        @DisplayName("returns 0 when ids array is empty")
        void returnsZeroWhenIdsArrayEmpty() {
            Long[] ids = {};
            when(monitorContentMapper.deleteMonitorContentByIds(ids)).thenReturn(0);

            int result = monitorContentService.deleteMonitorContentByIds(ids);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== checkMonitorContentCodeUnique Tests ====================

    @Nested
    @DisplayName("checkMonitorContentCodeUnique")
    class CheckMonitorContentCodeUnique {

        @Test
        @DisplayName("returns true when code is new (no existing record)")
        void returnsTrueWhenCodeIsNew() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setCode("NEW001");
            when(monitorContentMapper.checkMonitorContentCodeUnique("NEW001", 0L)).thenReturn(null);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns true when code belongs to same monitor content (id matches)")
        void returnsTrueWhenCodeBelongsToSameMonitorContent() {
            // When id matches, SQL's "id != #{id}" excludes that record
            // So no duplicate found, returns null -> service returns true (unique)
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(1L);
            monitorContent.setCode("rainfall_hour");
            MonitorContent existing = new MonitorContent();
            existing.setId(1L);
            existing.setCode("rainfall_hour");
            when(monitorContentMapper.checkMonitorContentCodeUnique("rainfall_hour", 1L)).thenReturn(null);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code belongs to different monitor content")
        void returnsFalseWhenCodeBelongsToDifferentMonitorContent() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(1L);
            monitorContent.setCode("rainfall_hour");
            MonitorContent existing = new MonitorContent();
            existing.setId(2L);
            existing.setCode("rainfall_hour");
            when(monitorContentMapper.checkMonitorContentCodeUnique("rainfall_hour", 1L)).thenReturn(existing);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true when monitor content id is null (new insert)")
        void returnsTrueWhenMonitorContentIdIsNull() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(null);
            monitorContent.setCode("rainfall_hour");
            when(monitorContentMapper.checkMonitorContentCodeUnique("rainfall_hour", 0L)).thenReturn(null);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("uses 0L as default id when id is null for uniqueness check")
        void usesZeroAsDefaultIdWhenIdIsNull() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(null);
            monitorContent.setCode("NEW_CODE");
            when(monitorContentMapper.checkMonitorContentCodeUnique("NEW_CODE", 0L)).thenReturn(null);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isTrue();
            verify(monitorContentMapper).checkMonitorContentCodeUnique("NEW_CODE", 0L);
        }

        @Test
        @DisplayName("returns false when duplicate code exists for different id")
        void returnsFalseWhenDuplicateCodeExists() {
            MonitorContent monitorContent = new MonitorContent();
            monitorContent.setId(5L);
            monitorContent.setCode("DUPLICATE");
            MonitorContent existing = new MonitorContent();
            existing.setId(10L);
            existing.setCode("DUPLICATE");
            when(monitorContentMapper.checkMonitorContentCodeUnique("DUPLICATE", 5L)).thenReturn(existing);

            boolean result = monitorContentService.checkMonitorContentCodeUnique(monitorContent);

            assertThat(result).isFalse();
        }
    }
}