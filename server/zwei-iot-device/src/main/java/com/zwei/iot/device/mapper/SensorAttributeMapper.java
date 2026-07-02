package com.zwei.iot.device.mapper;

import com.zwei.iot.device.domain.SensorAttribute;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 传感器属性Mapper接口
 *
 * @author zwei
 */
@Mapper
public interface SensorAttributeMapper {
    /**
     * 查询传感器属性列表
     *
     * @param sensorId 传感器ID
     * @return 属性列表
     */
    List<SensorAttribute> selectAttributeListBySensorId(@Param("sensorId") Long sensorId);

    /**
     * 批量根据传感器ID列表查询属性（避免逐传感器查询 N+1）。
     *
     * @param sensorIds 传感器ID列表
     * @return 属性列表
     */
    List<SensorAttribute> selectAttributeListBySensorIds(@Param("sensorIds") List<Long> sensorIds);

    /**
     * 根据设备ID查询所有传感器属性（JOIN device_sensor，批量查询避免 N+1）
     *
     * @param deviceId 设备ID
     * @return 属性列表
     */
    List<SensorAttribute> selectAttributeListByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据ID查询属性详情
     *
     * @param id 属性ID
     * @return 属性详情
     */
    SensorAttribute selectAttributeById(Long id);

    /**
     * 新增传感器属性
     *
     * @param attribute 属性信息
     * @return 影响行数
     */
    int insertAttribute(SensorAttribute attribute);

    /**
     * 修改传感器属性
     *
     * @param attribute 属性信息
     * @return 影响行数
     */
    int updateAttribute(SensorAttribute attribute);

    /**
     * 删除传感器属性
     *
     * @param id 属性ID
     * @return 影响行数
     */
    int deleteAttributeById(Long id);

    /**
     * 根据传感器ID删除属性
     *
     * @param sensorId 传感器ID
     * @return 影响行数
     */
    int deleteAttributeBySensorId(Long sensorId);

    /**
     * 批量根据设备ID列表删除传感器属性（物理删除，JOIN device_sensor）。
     *
     * @param deviceIds 设备ID列表
     * @return 影响行数
     */
    int deleteAttributeByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    /**
     * 批量新增传感器属性
     *
     * @param attributes 属性列表
     * @return 影响行数
     */
    int batchInsertAttribute(List<SensorAttribute> attributes);

    /**
     * 校验属性编码是否唯一
     *
     * @param attrCode 属性编码
     * @param sensorId 传感器ID
     * @param id       排除的属性ID（更新时使用）
     * @return 属性信息（null表示唯一）
     */
    SensorAttribute checkAttributeCodeUnique(@Param("attrCode") String attrCode, @Param("sensorId") Long sensorId, @Param("id") Long id);
}