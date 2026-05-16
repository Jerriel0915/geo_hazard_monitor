package com.zwei.iot.mapper;

import com.zwei.iot.domain.DeviceSensor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 传感器Mapper接口
 *
 * @author zwei
 */
@Mapper
public interface DeviceSensorMapper {
    /**
     * 查询设备下的传感器列表
     *
     * @param deviceId 设备ID
     * @return 传感器列表
     */
    List<DeviceSensor> selectSensorListByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据ID查询传感器详情
     *
     * @param id 传感器ID
     * @return 传感器详情
     */
    DeviceSensor selectSensorById(Long id);

    /**
     * 根据编码查询传感器
     *
     * @param sensorCode 传感器编码
     * @return 传感器详情
     */
    DeviceSensor selectSensorByCode(String sensorCode);

    /**
     * 新增传感器
     *
     * @param sensor 传感器信息
     * @return 影响行数
     */
    int insertSensor(DeviceSensor sensor);

    /**
     * 修改传感器
     *
     * @param sensor 传感器信息
     * @return 影响行数
     */
    int updateSensor(DeviceSensor sensor);

    /**
     * 删除传感器（逻辑删除）
     *
     * @param id 传感器ID
     * @return 影响行数
     */
    int deleteSensorById(Long id);

    /**
     * 根据设备ID删除传感器（逻辑删除）
     *
     * @param deviceId 设备ID
     * @return 影响行数
     */
    int deleteSensorByDeviceId(Long deviceId);

    /**
     * 校验传感器编码是否唯一
     *
     * @param sensorCode 传感器编码
     * @param id         排除的传感器ID（更新时使用）
     * @return 传感器信息（null表示唯一）
     */
    DeviceSensor checkSensorCodeUnique(@Param("sensorCode") String sensorCode, @Param("id") Long id);
}