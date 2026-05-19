package com.zwei.iot.video.service.impl;

import com.zwei.iot.video.domain.VideoDevice;
import com.zwei.iot.video.mapper.VideoDeviceMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VideoDeviceServiceImpl单元测试
 * <p>
 * 测试视频设备服务实现类的各项功能，包括：
 * - 分页查询视频设备列表
 * - 全量查询
 * - 按ID查询
 * - 新增
 * - 修改
 * - 删除（单个和批量）
 * - 编码唯一性校验
 *
 * @author zwei
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VideoDeviceServiceImpl Tests")
class VideoDeviceServiceImplTest {

    @Mock
    private VideoDeviceMapper videoDeviceMapper;

    private VideoDeviceServiceImpl videoDeviceService;

    @BeforeEach
    void setUp() {
        videoDeviceService = new VideoDeviceServiceImpl(videoDeviceMapper);
    }

    // ==================== selectVideoDevicePage Tests ====================

    @Nested
    @DisplayName("selectVideoDevicePage")
    class SelectVideoDevicePage {

        @Test
        @DisplayName("returns list from mapper")
        void returnsListFromMapper() {
            VideoDevice expected = new VideoDevice();
            expected.setId(1L);
            expected.setCode("VD001");
            expected.setName("摄像头A");
            when(videoDeviceMapper.selectVideoDeviceList(any(VideoDevice.class))).thenReturn(List.of(expected));

            List<VideoDevice> result = videoDeviceService.selectVideoDevicePage(expected, 1, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("VD001");
            verify(videoDeviceMapper).selectVideoDeviceList(expected);
        }

        @Test
        @DisplayName("returns empty list when no results")
        void returnsEmptyListWhenNoResults() {
            VideoDevice query = new VideoDevice();
            when(videoDeviceMapper.selectVideoDeviceList(any(VideoDevice.class))).thenReturn(List.of());

            List<VideoDevice> result = videoDeviceService.selectVideoDevicePage(query, 1, 10);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns multiple results")
        void returnsMultipleResults() {
            VideoDevice v1 = new VideoDevice();
            v1.setId(1L);
            v1.setCode("VD001");
            VideoDevice v2 = new VideoDevice();
            v2.setId(2L);
            v2.setCode("VD002");
            when(videoDeviceMapper.selectVideoDeviceList(any(VideoDevice.class))).thenReturn(Arrays.asList(v1, v2));

            List<VideoDevice> result = videoDeviceService.selectVideoDevicePage(new VideoDevice(), 1, 10);

            assertThat(result).hasSize(2);
        }
    }

    // ==================== selectVideoDeviceAll Tests ====================

    @Nested
    @DisplayName("selectVideoDeviceAll")
    class SelectVideoDeviceAll {

        @Test
        @DisplayName("returns all video devices")
        void returnsAllVideoDevices() {
            VideoDevice v1 = new VideoDevice();
            v1.setId(1L);
            v1.setCode("VD001");
            VideoDevice v2 = new VideoDevice();
            v2.setId(2L);
            v2.setCode("VD002");
            when(videoDeviceMapper.selectVideoDeviceAll()).thenReturn(Arrays.asList(v1, v2));

            List<VideoDevice> result = videoDeviceService.selectVideoDeviceAll();

            assertThat(result).hasSize(2);
            verify(videoDeviceMapper).selectVideoDeviceAll();
        }

        @Test
        @DisplayName("returns empty list when no video devices")
        void returnsEmptyListWhenNoVideoDevices() {
            when(videoDeviceMapper.selectVideoDeviceAll()).thenReturn(List.of());

            List<VideoDevice> result = videoDeviceService.selectVideoDeviceAll();

            assertThat(result).isEmpty();
        }
    }

    // ==================== selectVideoDeviceById Tests ====================

    @Nested
    @DisplayName("selectVideoDeviceById")
    class SelectVideoDeviceById {

        @Test
        @DisplayName("returns video device when found")
        void returnsVideoDeviceWhenFound() {
            VideoDevice expected = new VideoDevice();
            expected.setId(1L);
            expected.setCode("VD001");
            expected.setName("摄像头A");
            when(videoDeviceMapper.selectVideoDeviceById(1L)).thenReturn(expected);

            VideoDevice result = videoDeviceService.selectVideoDeviceById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCode()).isEqualTo("VD001");
        }

        @Test
        @DisplayName("returns null when video device not found")
        void returnsNullWhenVideoDeviceNotFound() {
            when(videoDeviceMapper.selectVideoDeviceById(999L)).thenReturn(null);

            VideoDevice result = videoDeviceService.selectVideoDeviceById(999L);

            assertThat(result).isNull();
        }
    }

    // ==================== insertVideoDevice Tests ====================

    @Nested
    @DisplayName("insertVideoDevice")
    class InsertVideoDevice {

        @Test
        @DisplayName("inserts successfully with all fields")
        void insertsSuccessfullyWithAllFields() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setCode("VD001");
            videoDevice.setName("摄像头A");
            videoDevice.setProtocolCode("RTSP");
            videoDevice.setStatus(1);
            when(videoDeviceMapper.insertVideoDevice(videoDevice)).thenReturn(1);

            int result = videoDeviceService.insertVideoDevice(videoDevice);

            assertThat(result).isEqualTo(1);
            verify(videoDeviceMapper).insertVideoDevice(videoDevice);
        }

        @Test
        @DisplayName("returns 1 when insert succeeds")
        void returnsOneWhenInsertSucceeds() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setCode("VD001");
            when(videoDeviceMapper.insertVideoDevice(videoDevice)).thenReturn(1);

            int result = videoDeviceService.insertVideoDevice(videoDevice);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("returns 0 when insert fails")
        void returnsZeroWhenInsertFails() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setCode("FAILED");
            when(videoDeviceMapper.insertVideoDevice(videoDevice)).thenReturn(0);

            int result = videoDeviceService.insertVideoDevice(videoDevice);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== updateVideoDevice Tests ====================

    @Nested
    @DisplayName("updateVideoDevice")
    class UpdateVideoDevice {

        @Test
        @DisplayName("delegates to mapper with updated fields")
        void delegatesToMapperWithUpdatedFields() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setId(1L);
            videoDevice.setName("更新后的摄像头");
            when(videoDeviceMapper.updateVideoDevice(videoDevice)).thenReturn(1);

            int result = videoDeviceService.updateVideoDevice(videoDevice);

            assertThat(result).isEqualTo(1);
            verify(videoDeviceMapper).updateVideoDevice(videoDevice);
        }

        @Test
        @DisplayName("returns 0 when update affects no rows")
        void returnsZeroWhenUpdateAffectsNoRows() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setId(999L);
            when(videoDeviceMapper.updateVideoDevice(videoDevice)).thenReturn(0);

            int result = videoDeviceService.updateVideoDevice(videoDevice);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteVideoDeviceById Tests ====================

    @Nested
    @DisplayName("deleteVideoDeviceById")
    class DeleteVideoDeviceById {

        @Test
        @DisplayName("deletes video device")
        void deletesVideoDevice() {
            when(videoDeviceMapper.deleteVideoDeviceById(1L)).thenReturn(1);

            int result = videoDeviceService.deleteVideoDeviceById(1L);

            assertThat(result).isEqualTo(1);
            verify(videoDeviceMapper).deleteVideoDeviceById(1L);
        }

        @Test
        @DisplayName("returns 0 when video device does not exist")
        void returnsZeroWhenVideoDeviceDoesNotExist() {
            when(videoDeviceMapper.deleteVideoDeviceById(999L)).thenReturn(0);

            int result = videoDeviceService.deleteVideoDeviceById(999L);

            assertThat(result).isEqualTo(0);
        }
    }

    // ==================== deleteVideoDeviceByIds Tests ====================

    @Nested
    @DisplayName("deleteVideoDeviceByIds")
    class DeleteVideoDeviceByIds {

        @Test
        @DisplayName("deletes multiple video devices")
        void deletesMultipleVideoDevices() {
            Long[] ids = {1L, 2L, 3L};
            when(videoDeviceMapper.deleteVideoDeviceByIds(ids)).thenReturn(3);

            int result = videoDeviceService.deleteVideoDeviceByIds(ids);

            assertThat(result).isEqualTo(3);
            verify(videoDeviceMapper).deleteVideoDeviceByIds(ids);
        }

        @Test
        @DisplayName("returns count of deleted rows")
        void returnsCountOfDeletedRows() {
            Long[] ids = {1L, 2L};
            when(videoDeviceMapper.deleteVideoDeviceByIds(ids)).thenReturn(2);

            int result = videoDeviceService.deleteVideoDeviceByIds(ids);

            assertThat(result).isEqualTo(2);
        }
    }

    // ==================== checkVideoDeviceCodeUnique Tests ====================

    @Nested
    @DisplayName("checkVideoDeviceCodeUnique")
    class CheckVideoDeviceCodeUnique {

        @Test
        @DisplayName("returns true when code is new (no existing record)")
        void returnsTrueWhenCodeIsNew() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setCode("NEW001");
            when(videoDeviceMapper.checkVideoDeviceCodeUnique("NEW001", null)).thenReturn(null);

            boolean result = videoDeviceService.checkVideoDeviceCodeUnique(videoDevice);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false when code belongs to different device")
        void returnsFalseWhenCodeBelongsToDifferentDevice() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setId(1L);
            videoDevice.setCode("VD001");
            VideoDevice existing = new VideoDevice();
            existing.setId(2L);
            existing.setCode("VD001");
            when(videoDeviceMapper.checkVideoDeviceCodeUnique("VD001", 1L)).thenReturn(existing);

            boolean result = videoDeviceService.checkVideoDeviceCodeUnique(videoDevice);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns true when device id is null (new insert)")
        void returnsTrueWhenDeviceIdIsNull() {
            VideoDevice videoDevice = new VideoDevice();
            videoDevice.setId(null);
            videoDevice.setCode("VD001");
            when(videoDeviceMapper.checkVideoDeviceCodeUnique("VD001", null)).thenReturn(null);

            boolean result = videoDeviceService.checkVideoDeviceCodeUnique(videoDevice);

            assertThat(result).isTrue();
        }
    }
}