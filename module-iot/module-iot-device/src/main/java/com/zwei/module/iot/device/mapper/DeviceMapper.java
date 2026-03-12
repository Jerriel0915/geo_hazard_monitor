package com.zwei.module.iot.device.mapper;

import com.zwei.module.iot.device.domain.Device;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 设备基本信息Mapper接口
 * 
 * @author zwei
 * @date 2025-09-05
 */
@Mapper
public interface DeviceMapper 
{
    /**
     * 查询设备基本信息
     * 
     * @param id 设备基本信息主键
     * @return 设备基本信息
     */
    Device selectDeviceById(Long id);

    /**
     * 查询设备基本信息列表
     * 
     * @param device 设备基本信息
     * @return 设备基本信息集合
     */
    List<Device> selectDeviceList(Device device);

    /**
     * 新增设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    int insertDevice(Device device);

    /**
     * 修改设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    int updateDevice(Device device);

    /**
     * 删除设备基本信息
     * 
     * @param id 设备基本信息主键
     * @return 结果
     */
    int deleteDeviceById(Long id);

    /**
     * 批量删除设备基本信息
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteDeviceByIds(Long[] ids);
}
