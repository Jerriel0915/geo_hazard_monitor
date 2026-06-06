package com.zwei.iot.device.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.dto.SensorAttributeRequest;
import com.zwei.iot.device.domain.dto.SensorCreateRequest;
import com.zwei.iot.device.domain.dto.SensorUpdateRequest;
import com.zwei.iot.device.service.IDeviceSensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 传感器管理Controller
 * <p>
 * 提供传感器的RESTful API接口，包括：
 * - 修改传感器（PUT /api/v1/sensors/{id}）
 * - 删除传感器（DELETE /api/v1/sensors/{id}）
 * - 新增传感器（POST /api/v1/devices/{deviceId}/sensors）
 * - 获取传感器详情（GET /api/v1/sensors/{id}）
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1")
public class SensorController extends BaseController {
    private final IDeviceSensorService sensorService;

    @Autowired
    public SensorController(IDeviceSensorService sensorService) {
        this.sensorService = sensorService;
    }

    /**
     * 获取传感器详情
     *
     * @param id 传感器ID
     * @return 传感器详情
     */
    @PreAuthorize("@ss.hasPermi('basic:sensor:query')")
    @GetMapping("/sensors/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        DeviceSensor sensor = sensorService.selectSensorById(id);
        if (sensor == null) {
            return error("传感器不存在");
        }
        return AjaxResult.success("成功", sensor);
    }

    /**
     * 修改传感器
     *
     * @param id     传感器ID
     * @param sensor 传感器信息（包含attrList）
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:sensor:edit')")
    @Log(title = "传感器", businessType = BusinessType.UPDATE)
    @PutMapping("/sensors/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody SensorUpdateRequest request) {
        DeviceSensor sensor = buildSensorForUpdate(id, request);
        sensor.setId(id);
        sensor.setUpdateBy(getUsername());
        int rows = sensorService.updateSensor(sensor, buildAttributes(request.getAttrList()));
        return rows > 0 ? AjaxResult.success("修改成功", null) : error("修改失败");
    }

    /**
     * 删除传感器（逻辑删除）
     *
     * @param id 传感器ID
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:sensor:remove')")
    @Log(title = "传感器", businessType = BusinessType.DELETE)
    @DeleteMapping("/sensors/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = sensorService.deleteSensorById(id);
        return rows > 0 ? AjaxResult.success("删除成功", null) : error("删除失败");
    }

    /**
     * 删除传感器属性（显式删除，替代 update 中的隐式删除）
     */
    @PreAuthorize("@ss.hasPermi('basic:sensor:edit')")
    @Log(title = "传感器属性", businessType = BusinessType.DELETE)
    @DeleteMapping("/sensors/{sensorId}/attributes/{attrId}")
    public AjaxResult removeAttribute(@PathVariable Long sensorId, @PathVariable Long attrId) {
        sensorService.deleteSensorAttribute(sensorId, attrId);
        return AjaxResult.success("删除成功");
    }

    /**
     * 新增传感器（为设备添加传感器）
     *
     * @param deviceId 设备ID
     * @param sensor   传感器信息（包含attrList）
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:sensor:add')")
    @Log(title = "传感器", businessType = BusinessType.INSERT)
    @PostMapping("/devices/{deviceId}/sensors")
    public AjaxResult add(@PathVariable Long deviceId, @Validated @RequestBody SensorCreateRequest request) {
        DeviceSensor sensor = buildSensorForCreate(request);
        sensor.setDeviceId(deviceId);
        sensor.setCreateBy(getUsername());
        if (!sensorService.checkSensorCodeUnique(sensor.getSensorCode(), 0L)) {
            return error("新增传感器'" + sensor.getSensorName() + "'失败，传感器编码已存在");
        }
        Long id = sensorService.insertSensor(sensor, buildAttributes(request.getAttrList()));
        return id != null
                ? AjaxResult.success("新增成功", Collections.singletonMap("id", id))
                : error("新增失败");
    }

    private DeviceSensor buildSensorForCreate(SensorCreateRequest request) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setSensorCode(request.getSensorCode().trim());
        sensor.setSensorName(request.getSensorName().trim());
        sensor.setMonitorTypeId(request.getMonitorTypeId());
        sensor.setStatus(request.getStatus());
        return sensor;
    }

    private DeviceSensor buildSensorForUpdate(Long id, SensorUpdateRequest request) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setId(id);
        sensor.setSensorName(request.getSensorName().trim());
        sensor.setStatus(request.getStatus());
        return sensor;
    }

    private List<SensorAttribute> buildAttributes(List<SensorAttributeRequest> attrRequests) {
        return attrRequests.stream().map(this::buildAttribute).toList();
    }

    private SensorAttribute buildAttribute(SensorAttributeRequest request) {
        SensorAttribute attribute = new SensorAttribute();
        attribute.setId(request.getId());
        attribute.setMonitorContentId(request.getMonitorContentId());
        attribute.setAttrCode(request.getAttrCode().trim());
        attribute.setAttrName(request.getAttrName().trim());
        attribute.setInitialValue(request.getInitialValue());
        attribute.setUnit(trimToNull(request.getUnit()));
        attribute.setRangeMin(request.getRangeMin());
        attribute.setRangeMax(request.getRangeMax());
        attribute.setIcon(trimToNull(request.getIcon()));
        return attribute;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
