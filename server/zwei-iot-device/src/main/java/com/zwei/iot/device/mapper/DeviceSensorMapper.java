package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.DeviceSensor;
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
     * @return 传感器信息（null表示唯一）
     */
    DeviceSensor checkSensorCodeUnique(@Param("sensorCode") String sensorCode, @Param("id") Long id);

    /**
     * 统计设备下未删除的传感器数量。
     *
     * @param deviceId 设备ID
     * @return 设备下未逻辑删除的传感器数量（空设备返回 0）
     */
    int countByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 查询当前最大传感器ID。
     * 仅统计未逻辑删除的记录。
     *
     * @return 最大 ID；表为空时返回 null
     */
    Long selectMaxId();

    // ==================== 统计查询 ====================

    int countAll();

    List<java.util.Map<String, Object>> countByStatus();

    List<java.util.Map<String, Object>> countByMonitorType();

    /** 更新传感器最后上报时间 */
    int updateLastReportTime(@Param("id") Long id, @Param("lastReportTime") String lastReportTime);

    /** 统计时间窗口内活跃传感器数（有数据上报） */
    int countActiveInWindow(@Param("windowMinutes") int windowMinutes);

    /** 统计有在线设备的传感器数（device_online_status.status=1） */
    int countByDeviceOnline();
}