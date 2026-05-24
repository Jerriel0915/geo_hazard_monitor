package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.mapper.HazardPointGroupMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HazardPointGroupServiceImpl 单元测试")
class HazardPointGroupServiceImplTest {

    @Mock
    private HazardPointGroupMapper mapper;

    @InjectMocks
    private HazardPointGroupServiceImpl service;

    @Nested
    @DisplayName("列表统计测试")
    class ListCountTests {

        @Test
        @DisplayName("查询列表时应批量回填隐患点数量")
        void selectList_shouldEnrichCounts() {
            HazardPointGroup first = buildGroup(1L, "G001", "第一分组");
            HazardPointGroup second = buildGroup(2L, "G002", "第二分组");
            when(mapper.selectHazardPointGroupList(any(HazardPointGroup.class))).thenReturn(List.of(first, second));
            when(mapper.countHazardPointsByGroupIds(List.of(1L, 2L))).thenReturn(
                    List.of(Map.of("groupId", 1L, "cnt", 3L))
            );

            List<HazardPointGroup> result = service.selectHazardPointGroupList(new HazardPointGroup());

            assertEquals(2, result.size());
            assertEquals(3, result.get(0).getCount());
            assertEquals(0, result.get(1).getCount());
        }
    }

    @Nested
    @DisplayName("修改测试")
    class UpdateTests {

        @Test
        @DisplayName("部分更新时未传编码和名称应保留原值")
        void updateGroup_shouldMergeExistingValues() {
            HazardPointGroup existing = buildGroup(1L, "G001", "原分组");
            existing.setDescription("原描述");
            HazardPointGroup incoming = new HazardPointGroup();
            incoming.setId(1L);
            incoming.setDescription("新描述");
            incoming.setSortOrder(10);
            when(mapper.selectHazardPointGroupById(1L)).thenReturn(existing);
            when(mapper.updateHazardPointGroup(any(HazardPointGroup.class))).thenReturn(1);

            int rows = service.updateHazardPointGroup(incoming);

            ArgumentCaptor<HazardPointGroup> captor = ArgumentCaptor.forClass(HazardPointGroup.class);
            verify(mapper).updateHazardPointGroup(captor.capture());
            HazardPointGroup actual = captor.getValue();
            assertEquals(1, rows);
            assertEquals("G001", actual.getCode());
            assertEquals("原分组", actual.getName());
            assertEquals("新描述", actual.getDescription());
            assertEquals(10, actual.getSortOrder());
        }

        @Test
        @DisplayName("分组不存在时修改应返回 0")
        void updateGroup_shouldReturnZeroWhenNotFound() {
            HazardPointGroup incoming = new HazardPointGroup();
            incoming.setId(99L);
            when(mapper.selectHazardPointGroupById(99L)).thenReturn(null);

            int rows = service.updateHazardPointGroup(incoming);

            assertEquals(0, rows);
            verify(mapper, never()).updateHazardPointGroup(any(HazardPointGroup.class));
        }
    }

    @Nested
    @DisplayName("删除测试")
    class DeleteTests {

        @Test
        @DisplayName("分组下存在隐患点时不允许删除")
        void deleteGroup_shouldThrowWhenGroupHasHazardPoints() {
            when(mapper.selectHazardPointGroupById(1L)).thenReturn(buildGroup(1L, "G001", "第一分组"));
            when(mapper.countHazardPointsByGroupId(1L)).thenReturn(2);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.deleteHazardPointGroupById(1L));

            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("不允许删除"));
            verify(mapper, never()).deleteHazardPointGroupById(1L);
        }

        @Test
        @DisplayName("分组不存在时删除应抛出 404 业务异常")
        void deleteGroup_shouldThrowWhenNotFound() {
            when(mapper.selectHazardPointGroupById(100L)).thenReturn(null);

            ServiceException exception = assertThrows(ServiceException.class,
                    () -> service.deleteHazardPointGroupById(100L));

            assertEquals(404, exception.getCode());
            assertEquals("分组不存在", exception.getMessage());
            verify(mapper, never()).countHazardPointsByGroupId(100L);
        }
    }

    @Nested
    @DisplayName("唯一性校验测试")
    class UniqueCheckTests {

        @Test
        @DisplayName("编码为空时应直接视为唯一")
        void checkUnique_shouldReturnTrueWhenCodeBlank() {
            HazardPointGroup group = new HazardPointGroup();

            boolean unique = service.checkGroupCodeUnique(group);

            assertTrue(unique);
        }

        @Test
        @DisplayName("相同记录更新自身编码时应视为唯一")
        void checkUnique_shouldAllowSameRecord() {
            HazardPointGroup group = buildGroup(1L, "G001", "第一分组");
            when(mapper.checkGroupCodeUnique("G001")).thenReturn(buildGroup(1L, "G001", "第一分组"));

            boolean unique = service.checkGroupCodeUnique(group);

            assertTrue(unique);
            verify(mapper).checkGroupCodeUnique("G001");
        }
    }

    private HazardPointGroup buildGroup(Long id, String code, String name) {
        HazardPointGroup group = new HazardPointGroup();
        group.setId(id);
        group.setCode(code);
        group.setName(name);
        assertNotNull(group);
        return group;
    }
}
