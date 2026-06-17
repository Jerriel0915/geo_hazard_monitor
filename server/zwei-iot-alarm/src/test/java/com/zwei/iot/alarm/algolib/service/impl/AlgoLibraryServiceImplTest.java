package com.zwei.iot.alarm.algolib.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.mapper.AlgoInfoMapper;
import com.zwei.iot.alarm.algolib.mapper.AlgoVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AlgoLibraryServiceImpl 单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlgoLibraryServiceImpl")
class AlgoLibraryServiceImplTest {

    @Mock private AlgoInfoMapper algoInfoMapper;
    @Mock private AlgoVersionMapper algoVersionMapper;

    private AlgoLibraryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AlgoLibraryServiceImpl(algoInfoMapper, algoVersionMapper);
    }

    @Nested
    @DisplayName("insert")
    class Insert {

        @Test
        @DisplayName("code 重复时抛 ServiceException")
        void duplicateCodeThrows() {
            when(algoInfoMapper.checkCodeUnique("ALGO_X", 0L))
                    .thenReturn(AlgoInfo.builder().id(99L).code("ALGO_X").build());

            AlgoInfo input = AlgoInfo.builder()
                    .code("ALGO_X").name("测试").createBy("admin").build();

            assertThatThrownBy(() -> service.insert(input))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("已存在");

            verify(algoInfoMapper, never()).insert(any());
        }

        @Test
        @DisplayName("code 唯一时正常插入，status 默认 1（启用）")
        void uniqueCodeInserts() {
            when(algoInfoMapper.checkCodeUnique("ALGO_NEW", 0L)).thenReturn(null);
            when(algoInfoMapper.insert(any(AlgoInfo.class))).thenAnswer(inv -> {
                inv.<AlgoInfo>getArgument(0).setId(1L);
                return 1;
            });

            AlgoInfo input = AlgoInfo.builder()
                    .code("ALGO_NEW").name("新算法").createBy("admin").build();

            int rows = service.insert(input);

            assertThat(rows).isEqualTo(1);
            assertThat(input.getId()).isEqualTo(1L);
            assertThat(input.getStatus()).isEqualTo(1);
            assertThat(input.getCreateTime()).isNotNull();
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("状态从 1 切换为 0，调用 mapper.update")
        void toggleToDisabled() {
            when(algoInfoMapper.update(any(AlgoInfo.class))).thenReturn(1);

            int rows = service.updateStatus(10L, 0, "admin");

            assertThat(rows).isEqualTo(1);
            verify(algoInfoMapper).update(argThat(a ->
                    a.getId().equals(10L)
                    && a.getStatus().equals(0)
                    && "admin".equals(a.getUpdateBy())));
        }
    }

    @Nested
    @DisplayName("deleteWithVersions")
    class DeleteWithVersions {

        @Test
        @DisplayName("删除算法时级联软删所有版本")
        void cascadesVersionSoftDelete() {
            when(algoInfoMapper.softDelete(10L)).thenReturn(1);
            when(algoVersionMapper.softDeleteByAlgoId(10L)).thenReturn(3);

            int rows = service.deleteWithVersions(10L);

            assertThat(rows).isEqualTo(1);
            verify(algoInfoMapper).softDelete(10L);
            verify(algoVersionMapper).softDeleteByAlgoId(10L);
        }

        @Test
        @DisplayName("算法不存在时返回 0，不调用版本 mapper")
        void notFound() {
            when(algoInfoMapper.softDelete(99L)).thenReturn(0);

            int rows = service.deleteWithVersions(99L);

            assertThat(rows).isEqualTo(0);
            verify(algoVersionMapper, never()).softDeleteByAlgoId(any());
        }
    }

    @Test
    @DisplayName("selectDetailById 填充版本列表")
    void selectDetailByIdFillsVersions() {
        AlgoInfo info = AlgoInfo.builder().id(1L).code("ALGO_X").name("X").build();
        when(algoInfoMapper.selectById(1L)).thenReturn(info);
        when(algoVersionMapper.selectByAlgoId(1L)).thenReturn(java.util.List.of());

        AlgoInfo result = service.selectDetailById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getVersions()).isNotNull().isEmpty();
    }
}
