package com.zwei.iot.device.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.service.IDeviceSensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
        return success(sensor);
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
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody DeviceSensor sensor) {
        // 设置ID
        sensor.setId(id);
        // 设置更新者
        sensor.setUpdateBy(getUsername());
        // 解析属性列表（attrList中每一项只包含attrCode,attrName,indicatorType等属性字段）
        List<SensorAttribute> attrList = sensor.getAttrList();
        // 执行修改
        int rows = sensorService.updateSensor(sensor, attrList);
        return rows > 0 ? success() : error("修改失败");
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
        return rows > 0 ? success() : error("删除失败");
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
    public AjaxResult add(@PathVariable Long deviceId, @Validated @RequestBody DeviceSensor sensor) {
        // 设置设备ID
        sensor.setDeviceId(deviceId);
        // 设置创建者
        sensor.setCreateBy(getUsername());
        // 校验编码唯一性
        if (!sensorService.checkSensorCodeUnique(sensor.getSensorCode(), 0L)) {
            return error("新增传感器'" + sensor.getSensorName() + "'失败，传感器编码已存在");
        }
        // 解析属性列表
        List<SensorAttribute> attrList = sensor.getAttrList();
        // 执行新增
        Long id = sensorService.insertSensor(sensor, attrList);
        return id != null ? success(id) : error("新增失败");
    }
}