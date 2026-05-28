package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.HttpStatus;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceService;
import com.zwei.iot.hazardpoint.domain.DeviceHazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BindDeviceRequest;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.domain.dto.InstallPosition;
import com.zwei.iot.hazardpoint.domain.dto.UnboundDeviceVO;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.service.IDeviceHazardPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备隐患点关联Service实现
 *
 * @author zwei
 */
@Service
public class DeviceHazardPointServiceImpl implements IDeviceHazardPointService {

    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final DeviceMapper deviceMapper;
    private final IDeviceService deviceService;
    private final HazardPointMapper hazardPointMapper;

    @Autowired
    public DeviceHazardPointServiceImpl(DeviceHazardPointMapper deviceHazardPointMapper,
                                       DeviceMapper deviceMapper,
                                       IDeviceService deviceService,
                                       HazardPointMapper hazardPointMapper) {
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.deviceMapper = deviceMapper;
        this.deviceService = deviceService;
        this.hazardPointMapper = hazardPointMapper;
    }

    /**
     * 获取隐患点已绑定的设备列表
     */
    @Override
    public List<BoundDeviceVO> getBoundDevices(Long hazardPointId) {
        ensureHazardPointExists(hazardPointId);
        // 1. 查询已绑定设备基础信息
        List<BoundDeviceVO> boundDevices = deviceHazardPointMapper.selectBoundDevicesByHazardPointId(hazardPointId);

        if (boundDevices == null || boundDevices.isEmpty()) {
            return boundDevices;
        }

        // 2. 批量查询各设备的传感器列表
        List<Long> deviceIds = boundDevices.stream()
                .map(BoundDeviceVO::getDeviceId)
                .toList();

        List<DeviceSensor> allSensors = loadSensors(deviceIds);

        // 3. 按设备ID分组
        Map<Long, List<DeviceSensor>> sensorsByDeviceId = allSensors.stream()
                .collect(Collectors.groupingBy(DeviceSensor::getDeviceId));

        // 4. 组装VO
        for (BoundDeviceVO vo : boundDevices) {
            List<DeviceSensor> sensors = sensorsByDeviceId.get(vo.getDeviceId());
            if (sensors != null && !sensors.isEmpty()) {
                List<BoundDeviceVO.SensorVO> sensorVOs = sensors.stream()
                        .map(sensor -> {
                            BoundDeviceVO.SensorVO sensorVO = new BoundDeviceVO.SensorVO();
                            sensorVO.setId(sensor.getId());
                            sensorVO.setName(sensor.getSensorName());
                            // 从传感器属性中获取图标路径（取第一个属性的图标）
                            sensorVO.setIconPath(
                                    sensor.getAttrList() != null && !sensor.getAttrList().isEmpty()
                                            ? sensor.getAttrList().get(0).getIcon() : null);
                            return sensorVO;
                        })
                        .collect(Collectors.toList());
                vo.setSensors(sensorVOs);
            } else {
                vo.setSensors(new ArrayList<>());
            }
        }

        return boundDevices;
    }

    /**
     * 获取未绑定设备列表
     */
    @Override
    public List<UnboundDeviceVO> getUnboundDevices(Long hazardPointId, String keyword) {
        ensureHazardPointExists(hazardPointId);
        // 1. 查询未绑定设备基础信息
        List<UnboundDeviceVO> unboundDevices = deviceHazardPointMapper.selectUnboundDevices(hazardPointId, keyword);

        if (unboundDevices == null || unboundDevices.isEmpty()) {
            return unboundDevices;
        }

        // 2. 批量查询各设备的传感器列表
        List<Long> deviceIds = unboundDevices.stream()
                .map(UnboundDeviceVO::getId)
                .toList();

        List<DeviceSensor> allSensors = loadSensors(deviceIds);

        // 3. 按设备ID分组
        Map<Long, List<DeviceSensor>> sensorsByDeviceId = allSensors.stream()
                .collect(Collectors.groupingBy(DeviceSensor::getDeviceId));

        // 4. 组装VO
        for (UnboundDeviceVO vo : unboundDevices) {
            List<DeviceSensor> sensors = sensorsByDeviceId.get(vo.getId());
            if (sensors != null && !sensors.isEmpty()) {
                List<UnboundDeviceVO.SensorVO> sensorVOs = sensors.stream()
                        .map(sensor -> {
                            UnboundDeviceVO.SensorVO sensorVO = new UnboundDeviceVO.SensorVO();
                            sensorVO.setId(sensor.getId());
                            sensorVO.setLabel(sensor.getSensorName());
                            // 从传感器属性中获取图标路径（取第一个属性的图标）
                            sensorVO.setIconPath(
                                    sensor.getAttrList() != null && !sensor.getAttrList().isEmpty()
                                            ? sensor.getAttrList().get(0).getIcon() : null);
                            return sensorVO;
                        })
                        .collect(Collectors.toList());
                vo.setChildren(sensorVOs);
            } else {
                vo.setChildren(new ArrayList<>());
            }
        }

        return unboundDevices;
    }

    /**
     * 绑定设备到隐患点
     * 采用按目标设备删除后重插策略，支持更新安装位置且不影响其他绑定关系
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", key = "#hazardPointId")
    })
    @Transactional(rollbackFor = Exception.class)
    public int bindDevices(Long hazardPointId, BindDeviceRequest request, String username) {
        ensureHazardPointExists(hazardPointId);
        List<Long> deviceIds = normalizeDeviceIds(request.getDeviceIds());
        validateDevicesExist(deviceIds);
        Map<Long, InstallPosition> positionMap = buildPositionMap(deviceIds, request.getInstallPositions());

        // 仅移除本次目标设备在该隐患点下的既有绑定，保留其他绑定关系。
        deviceHazardPointMapper.deleteByDeviceIdsAndHazardPointId(hazardPointId, deviceIds);

        List<DeviceHazardPoint> bindList = new ArrayList<>();
        for (Long deviceId : deviceIds) {
            DeviceHazardPoint bind = DeviceHazardPoint.builder()
                    .deviceId(deviceId)
                    .hazardPointId(hazardPointId)
                    .createBy(username)
                    .build();

            InstallPosition pos = positionMap.get(deviceId);
            if (pos != null) {
                bind.setInstallLongitude(pos.getInstallLongitude());
                bind.setInstallLatitude(pos.getInstallLatitude());
            }

            bindList.add(bind);
        }

        int rows = deviceHazardPointMapper.insertBatch(bindList);
        hazardPointMapper.refreshDeviceCountById(hazardPointId);
        return rows;
    }

    /**
     * 解绑设备
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "hazardPoint", key = "#hazardPointId")
    })
    @Transactional(rollbackFor = Exception.class)
    public int unbindDevices(Long hazardPointId, List<Long> deviceIds) {
        ensureHazardPointExists(hazardPointId);
        List<Long> normalizedDeviceIds = normalizeDeviceIds(deviceIds);
        validateDevicesExist(normalizedDeviceIds);

        int rows = deviceHazardPointMapper.deleteByDeviceIdsAndHazardPointId(hazardPointId, normalizedDeviceIds);
        hazardPointMapper.refreshDeviceCountById(hazardPointId);
        return rows;
    }

    private List<DeviceSensor> loadSensors(List<Long> deviceIds) {
        List<DeviceSensor> allSensors = new ArrayList<>();
        for (Long deviceId : deviceIds) {
            List<DeviceSensor> sensors = deviceService.selectSensorListByDeviceId(deviceId);
            if (sensors != null) {
                allSensors.addAll(sensors);
            }
        }
        return allSensors;
    }

    private void ensureHazardPointExists(Long hazardPointId) {
        if (hazardPointId == null) {
            throw new ServiceException("隐患点ID不能为空", HttpStatus.BAD_REQUEST);
        }
        if (hazardPointMapper.selectHazardPointById(hazardPointId) == null) {
            throw new ServiceException("隐患点不存在", HttpStatus.NOT_FOUND);
        }
    }

    private List<Long> normalizeDeviceIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new ServiceException("设备ID列表不能为空", HttpStatus.BAD_REQUEST);
        }
        if (deviceIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ServiceException("设备ID不能为空", HttpStatus.BAD_REQUEST);
        }
        Set<Long> uniqueIds = new LinkedHashSet<>(deviceIds);
        if (uniqueIds.size() != deviceIds.size()) {
            throw new ServiceException("设备ID列表存在重复值", HttpStatus.BAD_REQUEST);
        }
        return new ArrayList<>(uniqueIds);
    }

    private void validateDevicesExist(List<Long> deviceIds) {
        for (Long deviceId : deviceIds) {
            if (deviceMapper.selectDeviceById(deviceId) == null) {
                throw new ServiceException("设备不存在: " + deviceId, HttpStatus.NOT_FOUND);
            }
        }
    }

    private Map<Long, InstallPosition> buildPositionMap(List<Long> deviceIds, List<InstallPosition> installPositions) {
        if (installPositions == null || installPositions.isEmpty()) {
            return Map.of();
        }
        Set<Long> validIds = new LinkedHashSet<>(deviceIds);
        try {
            return installPositions.stream().collect(Collectors.toMap(position -> {
                Long deviceId = position.getDeviceId();
                if (!validIds.contains(deviceId)) {
                    throw new ServiceException("安装位置信息存在未绑定的设备ID: " + deviceId, HttpStatus.BAD_REQUEST);
                }
                return deviceId;
            }, position -> position));
        } catch (IllegalStateException ex) {
            throw new ServiceException("安装位置信息存在重复的设备ID", HttpStatus.BAD_REQUEST);
        }
    }
}
