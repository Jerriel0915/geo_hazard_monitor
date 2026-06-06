package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;

import java.util.List;

/**
 * 传感器Service接口
 *
 * @author zwei
 */
public interface IDeviceSensorService {
    /**
     * 根据设备ID查询传感器列表
     *
     * @param deviceId 设备ID
     * @return 传感器列表
     */
    List<DeviceSensor> selectSensorListByDeviceId(Long deviceId);

    /**
     * 根据ID查询传感器详情
     *
     * @param id 传感器ID
     * @return 传感器详情
     */
    DeviceSensor selectSensorById(Long id);

    /**
     * 新增传感器
     *
     * @param sensor   传感器信息
     * @param attrList 属性列表
     * @return 新增的传感器ID
     */
    Long insertSensor(DeviceSensor sensor, List<SensorAttribute> attrList);

    /**
     * 修改传感器
     *
     * @param sensor   传感器信息
     * @param attrList 属性列表
     * @return 影响行数
     */
    int updateSensor(DeviceSensor sensor, List<SensorAttribute> attrList);

    /**
     * 删除传感器（逻辑删除）
     *
     * @param id 传感器ID
     * @return 影响行数
     */
    int deleteSensorById(Long id);

    /**
     * 根据传感器条件查询传感器列表
     *
     * @param sensor 传感器查询条件
     * @return 传感器列表
     */
    List<DeviceSensor> selectSensorList(DeviceSensor sensor);

    /**
     * 校验传感器编码是否唯一
     *
     * @param sensorCode 传感器编码
     * @param id         排除的传感器ID（更新时使用）
     * @return true-唯一，false-已存在
     */
    boolean checkSensorCodeUnique(String sensorCode, Long id);

    /**
     * 更新传感器最后上报时间。
     * @param sensorId 传感器ID
     * @param lastReportTime 最后上报时间 (yyyy-MM-dd HH:mm:ss)
     */
    void updateLastReportTime(Long sensorId, String lastReportTime);

    /**
     * 删除传感器属性（显式删除，替代 updateSensor 中的隐式删除）。
     * @param sensorId 传感器ID
     * @param attrId   属性ID
     */
    void deleteSensorAttribute(Long sensorId, Long attrId);
}