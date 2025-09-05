package com.zwei.module.iot.device.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.module.iot.device.mapper.DeviceMapper;
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.service.IDeviceService;

/**
 * 设备基本信息Service业务层处理
 * 
 * @author zwei
 * @date 2025-09-05
 */
@Service
public class DeviceServiceImpl implements IDeviceService 
{
    @Autowired
    private DeviceMapper deviceMapper;

    /**
     * 查询设备基本信息
     * 
     * @param id 设备基本信息主键
     * @return 设备基本信息
     */
    @Override
    public Device selectDeviceById(Long id)
    {
        return deviceMapper.selectDeviceById(id);
    }

    /**
     * 查询设备基本信息列表
     * 
     * @param device 设备基本信息
     * @return 设备基本信息
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    @Override
    public int insertDevice(Device device)
    {
        return deviceMapper.insertDevice(device);
    }

    /**
     * 修改设备基本信息
     * 
     * @param device 设备基本信息
     * @return 结果
     */
    @Override
    public int updateDevice(Device device)
    {
        return deviceMapper.updateDevice(device);
    }

    /**
     * 批量删除设备基本信息
     * 
     * @param ids 需要删除的设备基本信息主键
     * @return 结果
     */
    @Override
    public int deleteDeviceByIds(Long[] ids)
    {
        return deviceMapper.deleteDeviceByIds(ids);
    }

    /**
     * 删除设备基本信息信息
     * 
     * @param id 设备基本信息主键
     * @return 结果
     */
    @Override
    public int deleteDeviceById(Long id)
    {
        return deviceMapper.deleteDeviceById(id);
    }
}
