package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.HttpStatus;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.video.domain.VideoDevice;
import com.zwei.iot.video.domain.VideoDeviceHazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BindVideoDeviceRequest;
import com.zwei.iot.video.domain.BoundVideoDeviceVO;
import com.zwei.iot.hazardpoint.domain.dto.VideoInstallPosition;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.video.mapper.VideoDeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.service.IVideoDeviceHazardPointService;
import com.zwei.iot.video.mapper.VideoDeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 视频设备隐患点关联Service实现
 */
@Service
public class VideoDeviceHazardPointServiceImpl implements IVideoDeviceHazardPointService {

    private final VideoDeviceHazardPointMapper videoDeviceHazardPointMapper;
    private final VideoDeviceMapper videoDeviceMapper;
    private final HazardPointMapper hazardPointMapper;

    @Autowired
    public VideoDeviceHazardPointServiceImpl(VideoDeviceHazardPointMapper videoDeviceHazardPointMapper,
                                             VideoDeviceMapper videoDeviceMapper,
                                             HazardPointMapper hazardPointMapper) {
        this.videoDeviceHazardPointMapper = videoDeviceHazardPointMapper;
        this.videoDeviceMapper = videoDeviceMapper;
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override
    public List<BoundVideoDeviceVO> getBoundVideoDevices(Long hazardPointId) {
        ensureHazardPointExists(hazardPointId);
        return videoDeviceHazardPointMapper.selectBoundVideoDevicesByHazardPointId(hazardPointId);
    }

    @Override
    public int bindVideoDevices(Long hazardPointId, BindVideoDeviceRequest request, String username) {
        ensureHazardPointExists(hazardPointId);
        List<Long> videoDeviceIds = normalizeVideoDeviceIds(request.getVideoDeviceIds());
        validateVideoDevicesExist(videoDeviceIds);
        Map<Long, VideoInstallPosition> positionMap = buildPositionMap(videoDeviceIds, request.getInstallPositions());

        List<VideoDeviceHazardPoint> bindList = new ArrayList<>(videoDeviceIds.size());
        for (Long videoDeviceId : videoDeviceIds) {
            VideoDeviceHazardPoint bind = VideoDeviceHazardPoint.builder()
                    .videoDeviceId(videoDeviceId)
                    .hazardPointId(hazardPointId)
                    .createBy(username)
                    .build();
            VideoInstallPosition position = positionMap.get(videoDeviceId);
            if (position != null) {
                bind.setInstallLongitude(position.getInstallLongitude());
                bind.setInstallLatitude(position.getInstallLatitude());
            }
            bindList.add(bind);
        }
        // 基于 uk_video_device_hazard_point 唯一键幂等 upsert，
        // 已绑定设备仅更新安装位置和更新者，与 DeviceHazardPointServiceImpl 策略一致
        return videoDeviceHazardPointMapper.insertOrUpdate(bindList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindVideoDevices(Long hazardPointId, List<Long> videoDeviceIds) {
        ensureHazardPointExists(hazardPointId);
        List<Long> normalizedIds = normalizeVideoDeviceIds(videoDeviceIds);
        validateVideoDevicesExist(normalizedIds);
        return videoDeviceHazardPointMapper.deleteByVideoDeviceIdsAndHazardPointId(hazardPointId, normalizedIds);
    }

    private void ensureHazardPointExists(Long hazardPointId) {
        if (hazardPointId == null) {
            throw new ServiceException("隐患点ID不能为空", HttpStatus.BAD_REQUEST);
        }
        if (hazardPointMapper.selectHazardPointById(hazardPointId) == null) {
            throw new ServiceException("隐患点不存在", HttpStatus.NOT_FOUND);
        }
    }

    private List<Long> normalizeVideoDeviceIds(List<Long> videoDeviceIds) {
        if (videoDeviceIds == null || videoDeviceIds.isEmpty()) {
            throw new ServiceException("视频设备ID列表不能为空", HttpStatus.BAD_REQUEST);
        }
        if (videoDeviceIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ServiceException("视频设备ID不能为空", HttpStatus.BAD_REQUEST);
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(videoDeviceIds);
        if (uniqueIds.size() != videoDeviceIds.size()) {
            throw new ServiceException("视频设备ID列表存在重复值", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(uniqueIds);
    }

    private void validateVideoDevicesExist(List<Long> videoDeviceIds) {
        List<VideoDevice> existing = videoDeviceMapper.selectVideoDeviceByIds(videoDeviceIds);
        Set<Long> existingIds = existing.stream().map(VideoDevice::getId).collect(Collectors.toSet());
        for (Long videoDeviceId : videoDeviceIds) {
            if (!existingIds.contains(videoDeviceId)) {
                throw new ServiceException("视频设备不存在: " + videoDeviceId, HttpStatus.NOT_FOUND);
            }
        }
    }

    private Map<Long, VideoInstallPosition> buildPositionMap(List<Long> videoDeviceIds,
                                                             List<VideoInstallPosition> installPositions) {
        if (installPositions == null || installPositions.isEmpty()) {
            return Map.of();
        }
        Set<Long> validIds = new LinkedHashSet<>(videoDeviceIds);
        try {
            return installPositions.stream().collect(Collectors.toMap(position -> {
                Long videoDeviceId = position.getVideoDeviceId();
                if (!validIds.contains(videoDeviceId)) {
                    throw new ServiceException("安装位置信息存在未绑定的视频设备ID: " + videoDeviceId, HttpStatus.BAD_REQUEST);
                }
                return videoDeviceId;
            }, position -> position));
        } catch (IllegalStateException ex) {
            throw new ServiceException("安装位置信息存在重复的视频设备ID", HttpStatus.BAD_REQUEST);
        }
    }
}
