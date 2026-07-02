package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.device.service.ITimeSeriesSchemaService;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 传感器全生命周期管理服务。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>CRUD</b>：新增传感器（含属性列表 + IoTDB schema 预创建）、修改、逻辑删除</li>
 *   <li><b>属性管理</b>：属性增删改，完整性校验（更新时 attrList 必须包含所有已有属性 ID）</li>
 *   <li><b>监测内容关联</b>：{@link #populateFromContent} 根据 monitorContentId 自动回填 attrCode 和 unit，
 *       无效/已停用的 monitorContentId 会显式抛 {@link ServiceException}</li>
 *   <li><b>唯一性校验</b>：sensorCode 全局唯一、同传感器下 attrCode 不重复</li>
 * </ul>
 *
 * <h3>IoTDB Schema 预创建</h3>
 * 新增传感器时调用 {@code timeSeriesSchemaService.createSensorSchema()} 提前建时序，
 * 将 DDL 从写入热路径移至注册冷路径。
 *
 * @author zwei
 */
@Service
public class DeviceSensorServiceImpl implements IDeviceSensorService {

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final IMonitorTypeService monitorTypeService;
    private final IMonitorContentService monitorContentService;
    private final ITimeSeriesSchemaService timeSeriesSchemaService;
    private final IProductTslService productTslService;

    @Autowired
    public DeviceSensorServiceImpl(DeviceMapper deviceMapper,
                                   DeviceSensorMapper sensorMapper,
                                   SensorAttributeMapper attributeMapper,
                                   IMonitorTypeService monitorTypeService,
                                   IMonitorContentService monitorContentService,
                                   ITimeSeriesSchemaService timeSeriesSchemaService,
                                   IProductTslService productTslService) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.monitorTypeService = monitorTypeService;
        this.monitorContentService = monitorContentService;
        this.timeSeriesSchemaService = timeSeriesSchemaService;
        this.productTslService = productTslService;
    }

    /**
     * 根据设备ID查询传感器列表
     */
    @Override
    public List<DeviceSensor> selectSensorListByDeviceId(Long deviceId) {
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(deviceId);
        if (sensors.isEmpty()) {
            return sensors;
        }
        List<Long> sensorIds = sensors.stream().map(DeviceSensor::getId).toList();
        List<SensorAttribute> allAttrs = attributeMapper.selectAttributeListBySensorIds(sensorIds);
        Map<Long, List<SensorAttribute>> attrsBySensor = allAttrs.stream()
                .collect(Collectors.groupingBy(SensorAttribute::getSensorId));
        for (DeviceSensor sensor : sensors) {
            sensor.setAttrList(attrsBySensor.getOrDefault(sensor.getId(), List.of()));
        }
        return sensors;
    }

    /**
     * 批量根据设备ID列表查询传感器列表（含属性，2 次查询避免 N+1）。
     */
    @Override
    public List<DeviceSensor> selectSensorListByDeviceIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceIds(deviceIds);
        if (sensors.isEmpty()) {
            return sensors;
        }
        List<Long> sensorIds = sensors.stream().map(DeviceSensor::getId).toList();
        List<SensorAttribute> allAttrs = attributeMapper.selectAttributeListBySensorIds(sensorIds);
        Map<Long, List<SensorAttribute>> attrsBySensor = allAttrs.stream()
                .collect(Collectors.groupingBy(SensorAttribute::getSensorId));
        for (DeviceSensor sensor : sensors) {
            sensor.setAttrList(attrsBySensor.getOrDefault(sensor.getId(), List.of()));
        }
        return sensors;
    }

    /**
     * 根据设备ID和传感器编码查询传感器（含 attrList）。
     */
    @Override
    public DeviceSensor selectSensorByDeviceIdAndCode(Long deviceId, String sensorCode) {
        DeviceSensor sensor = sensorMapper.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor != null) {
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
            sensor.setAttrList(attrs);
        }
        return sensor;
    }

    /**
     * 根据ID查询传感器详情
     */
    @Override
    public DeviceSensor selectSensorById(Long id) {
        DeviceSensor sensor = sensorMapper.selectSensorById(id);
        if (sensor != null) {
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(id);
            sensor.setAttrList(attrs);
        }
        return sensor;
    }

    /**
     * 新增传感器
     */
    @Override
    @Transactional
    public Long insertSensor(DeviceSensor sensor, List<SensorAttribute> attrList) {
        Device device = requireDevice(sensor.getDeviceId());
        if (!checkSensorCodeUnique(sensor.getDeviceId(), sensor.getSensorCode(), 0L)) {
            throw new ServiceException("传感器编码已存在");
        }
        fillDeviceFields(sensor, device);
        fillMonitorTypeFields(sensor, requireSensorMonitorType(sensor.getMonitorTypeId()));
        populateFromContent(attrList);
        validateAttributeList(attrList);

        sensorMapper.insertSensor(sensor);
        for (SensorAttribute attr : attrList) {
            attr.setSensorId(sensor.getId());
            attr.setCreateBy(sensor.getCreateBy());
            attributeMapper.insertAttribute(attr);
        }
        // 注册时预创建 IoTDB 时序 schema，将 DDL 从写入热路径提前到注册冷路径
        timeSeriesSchemaService.createSensorSchema(sensor.getDeviceId(), sensor.getSensorCode(),
                attrList.stream().map(SensorAttribute::getAttrCode).toList());
        productTslService.regenerate(sensor.getDeviceId());
        return sensor.getId();
    }

    /**
     * 修改传感器
     */
    @Override
    @Transactional
    public int updateSensor(DeviceSensor sensor, List<SensorAttribute> attrList) {
        DeviceSensor existing = sensorMapper.selectSensorById(sensor.getId());
        if (existing == null) {
            throw new ServiceException("传感器不存在");
        }

        fillDeviceFields(sensor, requireDevice(existing.getDeviceId()));
        Long monitorTypeId = sensor.getMonitorTypeId() != null ? sensor.getMonitorTypeId() : existing.getMonitorTypeId();
        fillMonitorTypeFields(sensor, requireSensorMonitorType(monitorTypeId));
        populateFromContent(attrList);
        validateAttributeList(attrList);

        int rows = sensorMapper.updateSensor(sensor);
        if (rows <= 0) {
            return rows;
        }

        List<SensorAttribute> existingAttrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
        Map<Long, SensorAttribute> existingAttrMap = new HashMap<>();
        for (SensorAttribute existingAttr : existingAttrs) {
            existingAttrMap.put(existingAttr.getId(), existingAttr);
        }

        Set<Long> retainedIds = new HashSet<>();
        for (SensorAttribute attr : attrList) {
            if (attr.getId() == null) {
                attr.setSensorId(sensor.getId());
                attr.setCreateBy(sensor.getUpdateBy());
                attributeMapper.insertAttribute(attr);
                continue;
            }

            SensorAttribute currentAttr = existingAttrMap.get(attr.getId());
            if (currentAttr == null) {
                throw new ServiceException("属性不存在或不属于当前传感器");
            }

            retainedIds.add(attr.getId());
            attr.setSensorId(sensor.getId());
            attr.setUpdateBy(sensor.getUpdateBy());
            attributeMapper.updateAttribute(attr);
        }

        // 校验：请求中的 attrList 必须包含所有已有属性 ID，不允许隐式删除
        Set<Long> existingIds = existingAttrMap.keySet();
        if (!retainedIds.containsAll(existingIds)) {
            existingIds.removeAll(retainedIds);
            throw new ServiceException("属性列表不完整，缺少属性 ID: " + existingIds + "，删除属性请使用 DELETE /api/v1/sensors/{sensorId}/attributes/{attrId}");
        }
        productTslService.regenerate(existing.getDeviceId());
        return rows;
    }

    /**
     * 删除传感器（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteSensorById(Long id) {
        DeviceSensor sensor = sensorMapper.selectSensorById(id);
        if (sensor == null) {
            throw new ServiceException("传感器不存在");
        }
        attributeMapper.deleteAttributeBySensorId(id);
        int rows = sensorMapper.deleteSensorById(id);
        productTslService.regenerate(sensor.getDeviceId());
        return rows;
    }

    /**
     * 校验传感器编码在指定设备内是否唯一
     */
    @Override
    public boolean checkSensorCodeUnique(Long deviceId, String sensorCode, Long id) {
        DeviceSensor result = sensorMapper.checkSensorCodeUnique(deviceId, sensorCode, id);
        return result == null;
    }

    /**
     * 预测指定设备下一个可用的传感器序号（设备下未删除传感器数 +1；空设备返回 1）
     */
    @Override
    public int getNextSensorCode(Long deviceId) {
        if (deviceId == null) {
            return 1;
        }
        return sensorMapper.countByDeviceId(deviceId) + 1;
    }

    /**
     * 根据传感器条件查询传感器列表
     */
    @Override
    public List<DeviceSensor> selectSensorList(DeviceSensor sensor) {
        return sensorMapper.selectSensorList(sensor);
    }

    private Device requireDevice(Long deviceId) {
        if (deviceId == null) {
            throw new ServiceException("设备ID不能为空");
        }
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    private MonitorType requireSensorMonitorType(Long monitorTypeId) {
        if (monitorTypeId == null) {
            throw new ServiceException("监测类型ID不能为空");
        }
        MonitorType monitorType = monitorTypeService.selectMonitorTypeById(monitorTypeId);
        if (monitorType == null) {
            throw new ServiceException("监测类型不存在");
        }
        return monitorType;
    }

    private void populateFromContent(List<SensorAttribute> attrList) {
        for (SensorAttribute attr : attrList) {
            if (attr.getMonitorContentId() == null) continue;
            MonitorContent mc = monitorContentService.selectMonitorContentById(attr.getMonitorContentId());
            if (mc == null) {
                throw new ServiceException("监测内容不存在或已停用: id=" + attr.getMonitorContentId());
            }
            if (attr.getAttrCode() == null || attr.getAttrCode().isBlank()) {
                attr.setAttrCode(mc.getCode());
            }
            if (attr.getUnit() == null || attr.getUnit().isBlank()) {
                attr.setUnit(mc.getUnit());
            }
        }
    }

    private void fillDeviceFields(DeviceSensor sensor, Device device) {
        sensor.setDeviceId(device.getId());
        sensor.setDeviceCode(device.getCode());
    }

    private void fillMonitorTypeFields(DeviceSensor sensor, MonitorType monitorType) {
        sensor.setMonitorTypeId(monitorType.getId());
        sensor.setMonitorTypeCode(monitorType.getCode());
        sensor.setMonitorTypeName(monitorType.getName());
    }

    private void validateAttributeList(List<SensorAttribute> attrList) {
        if (attrList == null || attrList.isEmpty()) {
            throw new ServiceException("属性列表不能为空");
        }

        Set<String> attrCodeSet = new HashSet<>();
        Set<Long> attrIdSet = new HashSet<>();
        for (SensorAttribute attr : attrList) {
            if (attr == null) {
                throw new ServiceException("属性列表存在空数据");
            }
            if (!attrCodeSet.add(attr.getAttrCode())) {
                throw new ServiceException("属性编码不能重复");
            }
            if (attr.getId() != null && !attrIdSet.add(attr.getId())) {
                throw new ServiceException("属性ID不能重复");
            }
            if (attr.getRangeMin() != null && attr.getRangeMax() != null
                    && attr.getRangeMin().compareTo(attr.getRangeMax()) > 0) {
                throw new ServiceException("属性最小值不能大于最大值");
            }
        }
    }

    @Override
    public void updateLastReportTime(Long sensorId, String lastReportTime) {
        sensorMapper.updateLastReportTime(sensorId, lastReportTime);
    }

    @Override
    @Transactional
    public void deleteSensorAttribute(Long sensorId, Long attrId) {
        SensorAttribute attr = attributeMapper.selectAttributeById(attrId);
        if (attr == null || !Objects.equals(attr.getSensorId(), sensorId)) {
            throw new ServiceException("属性不存在或不属于当前传感器");
        }
        attributeMapper.deleteAttributeById(attrId);
        DeviceSensor sensor = sensorMapper.selectSensorById(sensorId);
        if (sensor != null) {
            productTslService.regenerate(sensor.getDeviceId());
        }
    }

    @Override
    public List<String> findAttrCodesByDeviceAndSensor(Long deviceId, String sensorCode) {
        DeviceSensor sensor = sensorMapper.selectSensorByDeviceIdAndCode(deviceId, sensorCode);
        if (sensor == null) {
            return List.of();
        }
        List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
        return attrs.stream().map(SensorAttribute::getAttrCode).toList();
    }
}
