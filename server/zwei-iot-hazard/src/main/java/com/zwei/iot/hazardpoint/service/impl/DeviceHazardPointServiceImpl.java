package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.HttpStatus;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceService;
import com.zwei.iot.hazardpoint.domain.DeviceHazardPoint;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备-隐患点绑定关系管理服务。
 *
 * <h3>核心操作</h3>
 * <ul>
 *   <li><b>绑定设备</b>：基于 ON DUPLICATE KEY UPDATE 幂等绑定，已绑定设备仅更新安装位置和更新者</li>
 *   <li><b>解绑设备</b>：按 deviceIds 批量删除绑定，原子递减 hazard_point.device_count</li>
 *   <li><b>查询已绑定/未绑定设备</b>：支持关键词过滤（设备名/编码/传感器名模糊匹配）</li>
 * </ul>
 *
 * <h3>缓存策略</h3>
 * bind/unbind 操作均触发 {@code @CacheEvict(value = "hazardPoint", key = "#hazardPointId")}，
 * 确保隐患点缓存及时失效。
 *
 * <h3>device_count 维护</h3>
 * <ul>
 *   <li>解绑：原子递减 {@code GREATEST(device_count - N, 0)}，基于事务内实际删除行数</li>
 *   <li>绑定：子查询 {@code COUNT(*) FROM device_hazard_point}（并发安全考虑，避免 REPEATABLE READ 下快照漂移）</li>
 * </ul>
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
        ensureHazardPointExistsForView(hazardPointId);
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
        ensureHazardPointExistsForView(hazardPointId);
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
        ensureHazardPointActive(hazardPointId);
        List<Long> deviceIds = normalizeDeviceIds(request.getDeviceIds());
        validateDevicesExist(deviceIds);
        Map<Long, InstallPosition> positionMap = buildPositionMap(deviceIds, request.getInstallPositions());

        // 使用 ON DUPLICATE KEY UPDATE 基于唯一键幂等操作：
        // 已绑定设备仅更新安装位置和更新者，新绑定设备插入记录。
        // 注意：此处使用子查询 COUNT 而非预查+原子递增，因为 REPEATABLE READ
        // 下并发 bind 无法通过快照读准确计算新增数，存在 device_count 漂移风险。
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

        int rows = deviceHazardPointMapper.insertOrUpdate(bindList);
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
        ensureHazardPointActive(hazardPointId);
        List<Long> normalizedDeviceIds = normalizeDeviceIds(deviceIds);
        validateDevicesExist(normalizedDeviceIds);

        int rows = deviceHazardPointMapper.deleteByDeviceIdsAndHazardPointId(hazardPointId, normalizedDeviceIds);
        if (rows > 0) {
            hazardPointMapper.decrementDeviceCount(hazardPointId, rows);
        }
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

    /** 仅校验隐患点存在且未删除（查询操作使用，允许停测/完结状态查看） */
    private void ensureHazardPointExistsForView(Long hazardPointId) {
        if (hazardPointId == null) {
            throw new ServiceException("隐患点ID不能为空", HttpStatus.BAD_REQUEST);
        }
        HazardPoint hazardPoint = hazardPointMapper.selectHazardPointById(hazardPointId);
        if (hazardPoint == null) {
            throw new ServiceException("隐患点不存在", HttpStatus.NOT_FOUND);
        }
        if (!"0".equals(hazardPoint.getDelFlag())) {
            throw new ServiceException("隐患点已删除", HttpStatus.BAD_REQUEST);
        }
    }

    /** 校验隐患点存在且可操作（修改操作使用，额外要求 status=监测中） */
    private void ensureHazardPointActive(Long hazardPointId) {
        ensureHazardPointExistsForView(hazardPointId);
        HazardPoint hazardPoint = hazardPointMapper.selectHazardPointById(hazardPointId);
        if (!Integer.valueOf(1).equals(hazardPoint.getStatus())) {
            throw new ServiceException("隐患点已停测或完结，无法修改设备绑定", HttpStatus.BAD_REQUEST);
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
        List<Device> devices = deviceMapper.selectDeviceByIds(deviceIds);
        if (devices.size() != deviceIds.size()) {
            Set<Long> foundIds = devices.stream().map(Device::getId).collect(Collectors.toCollection(LinkedHashSet::new));
            String missing = deviceIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new ServiceException("设备不存在或已删除: " + missing, HttpStatus.NOT_FOUND);
        }
        for (Device device : devices) {
            if (!Integer.valueOf(1).equals(device.getAuthStatus())) {
                throw new ServiceException("设备账号已禁用: " + device.getCode(), HttpStatus.BAD_REQUEST);
            }
            if (!Integer.valueOf(1).equals(device.getStatus())) {
                throw new ServiceException("设备已停用: " + device.getCode(), HttpStatus.BAD_REQUEST);
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
