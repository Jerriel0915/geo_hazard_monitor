package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;

import java.util.List;

/**
 * 设备Service接口
 *
 * @author zwei
 */
public interface IDeviceService {
    /**
     * 分页查询设备列表
     *
     * @param device   设备查询条件
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 设备列表
     */
    List<Device> selectDevicePage(Device device, int pageNum, int pageSize);

    /**
     * 查询所有设备列表
     *
     * @return 所有设备列表
     */
    List<Device> selectDeviceAll();

    /**
     * 根据ID查询设备详情
     *
     * @param id 设备ID
     * @return 设备详情
     */
    Device selectDeviceById(Long id);

    /**
     * 新增设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int insertDevice(Device device);

    /**
     * 修改设备
     *
     * @param device 设备信息
     * @return 影响行数
     */
    int updateDevice(Device device);

    /**
     * 删除设备（逻辑删除）
     *
     * @param id 设备ID
     * @return 影响行数
     */
    int deleteDeviceById(Long id);

    /**
     * 批量删除设备（逻辑删除）
     *
     * @param ids 需要删除的设备ID数组
     * @return 影响行数
     */
    int deleteDeviceByIds(Long[] ids);

    /**
     * 复制设备
     *
     * @param id 设备ID
     * @return 新设备ID
     */
    Long copyDevice(Long id);

    /**
     * 校验设备编码是否唯一
     *
     * @param device 设备信息
     * @return true-唯一，false-已存在
     */
    boolean checkDeviceCodeUnique(Device device);

    /**
     * 获取设备传感器列表
     *
     * @param deviceId 设备ID
     * @return 传感器列表
     */
    List<DeviceSensor> selectSensorListByDeviceId(Long deviceId);
}