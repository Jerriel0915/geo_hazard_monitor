package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.hazardpoint.domain.DeviceHazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.BindDeviceRequest;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.domain.dto.InstallPosition;
import com.zwei.iot.hazardpoint.domain.dto.UnboundDeviceVO;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.service.IDeviceHazardPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 设备隐患点关联Service实现
 *
 * @author zwei
 */
@Service
public class DeviceHazardPointServiceImpl implements IDeviceHazardPointService {

    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final DeviceSensorMapper deviceSensorMapper;

    @Autowired
    public DeviceHazardPointServiceImpl(DeviceHazardPointMapper deviceHazardPointMapper,
                                       DeviceSensorMapper deviceSensorMapper) {
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.deviceSensorMapper = deviceSensorMapper;
    }

    /**
     * 获取隐患点已绑定的设备列表
     */
    @Override
    public List<BoundDeviceVO> getBoundDevices(Long hazardPointId) {
        // 1. 查询已绑定设备基础信息
        List<BoundDeviceVO> boundDevices = deviceHazardPointMapper.selectBoundDevicesByHazardPointId(hazardPointId);

        if (boundDevices == null || boundDevices.isEmpty()) {
            return boundDevices;
        }

        // 2. 批量查询各设备的传感器列表
        List<Long> deviceIds = boundDevices.stream()
                .map(BoundDeviceVO::getDeviceId)
                .toList();

        List<DeviceSensor> allSensors = new ArrayList<>();
        for (Long deviceId : deviceIds) {
            List<DeviceSensor> sensors = deviceSensorMapper.selectSensorListByDeviceId(deviceId);
            if (sensors != null) {
                allSensors.addAll(sensors);
            }
        }

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
        // 1. 查询未绑定设备基础信息
        List<UnboundDeviceVO> unboundDevices = deviceHazardPointMapper.selectUnboundDevices(hazardPointId, keyword);

        if (unboundDevices == null || unboundDevices.isEmpty()) {
            return unboundDevices;
        }

        // 2. 批量查询各设备的传感器列表
        List<Long> deviceIds = unboundDevices.stream()
                .map(UnboundDeviceVO::getId)
                .toList();

        List<DeviceSensor> allSensors = new ArrayList<>();
        for (Long deviceId : deviceIds) {
            List<DeviceSensor> sensors = deviceSensorMapper.selectSensorListByDeviceId(deviceId);
            if (sensors != null) {
                allSensors.addAll(sensors);
            }
        }

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
     * 采用先删除再插入策略，支持更新设备的安装位置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int bindDevices(Long hazardPointId, BindDeviceRequest request, String username) {
        if (hazardPointId == null) {
            throw new IllegalArgumentException("隐患点ID不能为空");
        }
        if (request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
            throw new IllegalArgumentException("设备ID列表不能为空");
        }

        // 1. 先删除该隐患点现有的所有绑定记录
        deviceHazardPointMapper.deleteByHazardPointId(hazardPointId);

        // 2. 构建安装位置映射
        Map<Long, InstallPosition> positionMap = null;
        if (request.getInstallPositions() != null) {
            positionMap = request.getInstallPositions().stream()
                    .filter(p -> p.getDeviceId() != null)
                    .collect(Collectors.toMap(InstallPosition::getDeviceId, p -> p));
        }

        // 3. 批量插入新的绑定记录
        List<DeviceHazardPoint> bindList = new ArrayList<>();
        for (Long deviceId : request.getDeviceIds()) {
            DeviceHazardPoint bind = DeviceHazardPoint.builder()
                    .deviceId(deviceId)
                    .hazardPointId(hazardPointId)
                    .createBy(username)
                    .build();

            // 设置安装位置
            if (positionMap != null && positionMap.containsKey(deviceId)) {
                InstallPosition pos = positionMap.get(deviceId);
                bind.setInstallLongitude(pos.getInstallLongitude());
                bind.setInstallLatitude(pos.getInstallLatitude());
            }

            bindList.add(bind);
        }

        return deviceHazardPointMapper.insertBatch(bindList);
    }

    /**
     * 解绑设备
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int unbindDevices(Long hazardPointId, List<Long> deviceIds) {
        if (hazardPointId == null) {
            throw new IllegalArgumentException("隐患点ID不能为空");
        }
        if (deviceIds == null || deviceIds.isEmpty()) {
            throw new IllegalArgumentException("设备ID列表不能为空");
        }

        return deviceHazardPointMapper.deleteByDeviceIdsAndHazardPointId(hazardPointId, deviceIds);
    }
}
