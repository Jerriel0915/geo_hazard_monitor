package com.zwei.iot.hazardPoint.service.impl;

import com.zwei.common.constant.IotConstants;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.cache.config.CacheWarmupTaskRegistry;
import com.zwei.iot.cache.service.IotCacheService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.service.impl.HazardPointServiceImpl;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HazardPointServiceImpl Tests")
class HazardPointServiceImplTest {

    @Mock
    private HazardPointMapper hazardPointMapper;

    @Mock
    private IotCacheService cacheService;

    @Mock
    private CacheWarmupTaskRegistry registry;

    private HazardPointServiceImpl hazardPointService;

    @BeforeEach
    void setUp() {
        hazardPointService = new HazardPointServiceImpl(hazardPointMapper, cacheService, registry);
    }

    @Nested
    @DisplayName("selectHazardPointList")
    class SelectHazardPointList {

        @Test
        @DisplayName("returns list from mapper")
        void returnsListFromMapper() {
            HazardPoint expected = new HazardPoint(1L);
            expected.setName("Test Point");
            when(hazardPointMapper.selectHazardPointList(expected)).thenReturn(List.of(expected));

            List<HazardPoint> result = hazardPointService.selectHazardPointList(expected);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Test Point");
            verify(hazardPointMapper).selectHazardPointList(expected);
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() {
            when(hazardPointMapper.selectHazardPointList(new HazardPoint())).thenReturn(List.of());

            List<HazardPoint> result = hazardPointService.selectHazardPointList(new HazardPoint());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectHazardPointById")
    class SelectHazardPointById {

        @Test
        @DisplayName("returns hazard point when exists")
        void returnsHazardPointWhenExists() {
            HazardPoint expected = new HazardPoint(100L);
            when(hazardPointMapper.selectHazardPointById(100L)).thenReturn(expected);

            HazardPoint result = hazardPointService.selectHazardPointById(100L);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("returns null when not found")
        void returnsNullWhenNotFound() {
            when(hazardPointMapper.selectHazardPointById(999L)).thenReturn(null);

            HazardPoint result = hazardPointService.selectHazardPointById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("insertHazardPoint")
    class InsertHazardPoint {

        @Test
        @DisplayName("inserts successfully when code is unique")
        void insertsSuccessfullyWhenCodeIsUnique() {
            HazardPoint hazardPoint = new HazardPoint();
            hazardPoint.setCode("HP001");
            hazardPoint.setName("New Point");
            when(hazardPointMapper.checkHazardPointCodeUnique("HP001")).thenReturn(null);
            when(hazardPointMapper.insertHazardPoint(hazardPoint)).thenReturn(1);

            int result = hazardPointService.insertHazardPoint(hazardPoint);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).insertHazardPoint(hazardPoint);
        }

        @Test
        @DisplayName("sets default status to monitoring when status is null")
        void setsDefaultStatusToMonitoringWhenNull() {
            HazardPoint hazardPoint = new HazardPoint();
            hazardPoint.setCode("HP002");
            when(hazardPointMapper.checkHazardPointCodeUnique("HP002")).thenReturn(null);
            when(hazardPointMapper.insertHazardPoint(hazardPoint)).thenReturn(1);

            hazardPointService.insertHazardPoint(hazardPoint);

            assertThat(hazardPoint.getStatus()).isEqualTo(IotConstants.HAZARD_POINT_STATUS_MONITORING);
        }

        @Test
        @DisplayName("throws exception when code already exists")
        void throwsExceptionWhenCodeAlreadyExists() {
            HazardPoint hazardPoint = new HazardPoint();
            hazardPoint.setCode("HP001");
            when(hazardPointMapper.checkHazardPointCodeUnique("HP001")).thenReturn(new HazardPoint(1L));

            assertThatThrownBy(() -> hazardPointService.insertHazardPoint(hazardPoint))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("隐患点编号已存在");
        }

        @Test
        @DisplayName("does not override status when already set")
        void doesNotOverrideStatusWhenAlreadySet() {
            HazardPoint hazardPoint = new HazardPoint();
            hazardPoint.setCode("HP003");
            hazardPoint.setStatus(IotConstants.HAZARD_POINT_STATUS_PAUSED);
            when(hazardPointMapper.checkHazardPointCodeUnique("HP003")).thenReturn(null);
            when(hazardPointMapper.insertHazardPoint(hazardPoint)).thenReturn(1);

            hazardPointService.insertHazardPoint(hazardPoint);

            assertThat(hazardPoint.getStatus()).isEqualTo(IotConstants.HAZARD_POINT_STATUS_PAUSED);
        }
    }

    @Nested
    @DisplayName("updateHazardPoint")
    class UpdateHazardPoint {

        @Test
        @DisplayName("delegates to mapper")
        void delegatesToMapper() {
            HazardPoint hazardPoint = new HazardPoint(1L);
            when(hazardPointMapper.updateHazardPoint(hazardPoint)).thenReturn(1);

            int result = hazardPointService.updateHazardPoint(hazardPoint);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).updateHazardPoint(hazardPoint);
        }
    }

    @Nested
    @DisplayName("deleteHazardPointById")
    class DeleteHazardPointById {

        @Test
        @DisplayName("delegates to mapper")
        void delegatesToMapper() {
            when(hazardPointMapper.deleteHazardPointById(1L)).thenReturn(1);

            int result = hazardPointService.deleteHazardPointById(1L);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).deleteHazardPointById(1L);
        }
    }

    @Nested
    @DisplayName("deleteHazardPointByIds")
    class DeleteHazardPointByIds {

        @Test
        @DisplayName("delegates to mapper with array")
        void delegatesToMapperWithArray() {
            Long[] ids = {1L, 2L, 3L};
            when(hazardPointMapper.deleteHazardPointByIds(ids)).thenReturn(3);

            int result = hazardPointService.deleteHazardPointByIds(ids);

            assertThat(result).isEqualTo(3);
            verify(hazardPointMapper).deleteHazardPointByIds(ids);
        }
    }

    @Nested
    @DisplayName("checkHazardPointCodeUnique")
    class CheckHazardPointCodeUnique {

        @Test
        @DisplayName("returns true when code is null or blank")
        void returnsTrueWhenCodeIsNullOrBlank() {
            assertThat(hazardPointService.checkHazardPointCodeUnique(null)).isTrue();
            assertThat(hazardPointService.checkHazardPointCodeUnique("")).isTrue();
            assertThat(hazardPointService.checkHazardPointCodeUnique("   ")).isTrue();
        }

        @Test
        @DisplayName("returns true when code does not exist")
        void returnsTrueWhenCodeDoesNotExist() {
            when(hazardPointMapper.checkHazardPointCodeUnique("NEW001")).thenReturn(null);

            boolean result = hazardPointService.checkHazardPointCodeUnique("NEW001");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code already exists")
        void returnsFalseWhenCodeAlreadyExists() {
            when(hazardPointMapper.checkHazardPointCodeUnique("EXIST001")).thenReturn(new HazardPoint(1L));

            boolean result = hazardPointService.checkHazardPointCodeUnique("EXIST001");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateHazardPointPause")
    class UpdateHazardPointPause {

        @Test
        @DisplayName("sets status to paused when pause is true")
        void setsStatusToPausedWhenPauseIsTrue() {
            when(hazardPointMapper.updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_PAUSED)).thenReturn(1);

            int result = hazardPointService.updateHazardPointPause(1L, true);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_PAUSED);
            verify(cacheService).evictHazardPoint(1L);
        }

        @Test
        @DisplayName("sets status to monitoring when pause is false")
        void setsStatusToMonitoringWhenPauseIsFalse() {
            when(hazardPointMapper.updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_MONITORING)).thenReturn(1);

            int result = hazardPointService.updateHazardPointPause(1L, false);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_MONITORING);
            verify(cacheService).evictHazardPoint(1L);
        }
    }

    @Nested
    @DisplayName("completeHazardPoint")
    class CompleteHazardPoint {

        @Test
        @DisplayName("sets status to completed")
        void setsStatusToCompleted() {
            when(hazardPointMapper.updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_COMPLETED)).thenReturn(1);

            int result = hazardPointService.completeHazardPoint(1L);

            assertThat(result).isEqualTo(1);
            verify(hazardPointMapper).updateHazardPointStatus(1L, IotConstants.HAZARD_POINT_STATUS_COMPLETED);
            verify(cacheService).evictHazardPoint(1L);
        }
    }

    @Nested
    @DisplayName("batchOperateHazardPoint")
    class BatchOperateHazardPoint {

        @Test
        @DisplayName("throws exception when ids is null or empty")
        void throwsExceptionWhenIdsIsNullOrEmpty() {
            assertThatThrownBy(() -> hazardPointService.batchOperateHazardPoint(null, "pause"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("请选择要操作的隐患点");

            assertThatThrownBy(() -> hazardPointService.batchOperateHazardPoint(new Long[]{}, "pause"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("请选择要操作的隐患点");
        }

        @Test
        @DisplayName("throws exception for invalid operation type")
        void throwsExceptionForInvalidOperationType() {
            assertThatThrownBy(() -> hazardPointService.batchOperateHazardPoint(new Long[]{1L}, "invalid"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("无效的操作类型");
        }

        @Test
        @DisplayName("pauses multiple hazard points")
        void pausesMultipleHazardPoints() {
            Long[] ids = {1L, 2L};
            when(hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), IotConstants.HAZARD_POINT_STATUS_PAUSED)).thenReturn(2);

            int result = hazardPointService.batchOperateHazardPoint(ids, IotConstants.OPERATION_PAUSE);

            assertThat(result).isEqualTo(2);
            verify(hazardPointMapper).batchUpdateHazardPointStatus(Arrays.asList(ids), IotConstants.HAZARD_POINT_STATUS_PAUSED);
            verify(cacheService).evictHazardPointList(ids);
        }

        @Test
        @DisplayName("resumes multiple hazard points")
        void resumesMultipleHazardPoints() {
            Long[] ids = {1L, 2L, 3L};
            when(hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), IotConstants.HAZARD_POINT_STATUS_MONITORING)).thenReturn(3);

            int result = hazardPointService.batchOperateHazardPoint(ids, IotConstants.OPERATION_RESUME);

            assertThat(result).isEqualTo(3);
            verify(cacheService).evictHazardPointList(ids);
        }

        @Test
        @DisplayName("completes multiple hazard points")
        void completesMultipleHazardPoints() {
            Long[] ids = {1L};
            when(hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), IotConstants.HAZARD_POINT_STATUS_COMPLETED)).thenReturn(1);

            int result = hazardPointService.batchOperateHazardPoint(ids, IotConstants.OPERATION_COMPLETE);

            assertThat(result).isEqualTo(1);
            verify(cacheService).evictHazardPointList(ids);
        }
    }
}
