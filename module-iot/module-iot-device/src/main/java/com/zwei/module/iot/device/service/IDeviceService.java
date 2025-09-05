package com.zwei.module.iot.device.service;

import java.util.List;
import com.zwei.module.iot.device.domain.Device;

/**
 * 设备基本信息Service接口
 * 
 * @author zwei
 * @date 2025-09-05
 */
public interface IDeviceService 
{
    /**
     * 查询设备基本信息
     * 
     * @param id 设备基本信息主键
     * @return 设备基本信息
     */
    public Device selectDeviceById(Long id);

    /**
     * 查询设备基本信息列表
     * 
     * @param device 设备基本信息
     * @return 设备基本信息集合
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 新增设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 批量删除设备基本信息
     * 
     * @param ids 需要删除的设备基本信息主键集合
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] ids);

    /**
     * 删除设备基本信息信息
     * 
     * @param id 设备基本信息主键
     * @return 结果
     */
    public int deleteDeviceById(Long id);
}
