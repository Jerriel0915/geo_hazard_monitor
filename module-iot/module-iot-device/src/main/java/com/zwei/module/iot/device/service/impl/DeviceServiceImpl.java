package com.zwei.module.iot.device.service.impl;

import com.zwei.common.core.redis.RedisCache;
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.mapper.DeviceMapper;
import com.zwei.module.iot.device.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 设备基本信息Service业务层处理
 *
 * @author zwei
 * @date 2025-09-05
 */
@Service
public class DeviceServiceImpl implements IDeviceService {
    private final DeviceMapper deviceMapper;
    private final RedisCache redisCache;

    private static final String CACHE_KEY_PREFIX = "iot:device:id:";

    @Autowired
    DeviceServiceImpl(DeviceMapper deviceMapper, RedisCache redisCache) {
        this.deviceMapper = deviceMapper;
        this.redisCache = redisCache;
    }

    /**
     * 查询设备基本信息
     *
     * @param id 设备基本信息主键
     * @return 设备基本信息
     */
    @Override
    public Device selectDeviceById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        Device device = redisCache.getCacheObject(cacheKey);
        if (device != null) {
            return device;
        }
        device = deviceMapper.selectDeviceById(id);
        if (device != null) {
            redisCache.setCacheObject(cacheKey, device, 1, TimeUnit.HOURS);
        }
        return device;
    }

    /**
     * 查询设备基本信息列表
     *
     * @param device 设备基本信息
     * @return 设备基本信息
     */
    @Override
    public List<Device> selectDeviceList(Device device) {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 新增设备基本信息
     *
     * @param device 设备基本信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertDevice(Device device) {
        return deviceMapper.insertDevice(device);
    }

    /**
     * 修改设备基本信息
     *
     * @param device 设备基本信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDevice(Device device) {
        int rows = deviceMapper.updateDevice(device);
        if (rows > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + device.getId());
        }
        return rows;
    }

    /**
     * 批量删除设备基本信息
     *
     * @param ids 需要删除的设备基本信息主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDeviceByIds(Long[] ids) {
        int rows = deviceMapper.deleteDeviceByIds(ids);
        if (rows > 0) {
            for (Long id : ids) {
                redisCache.deleteObject(CACHE_KEY_PREFIX + id);
            }
        }
        return rows;
    }

    /**
     * 删除设备基本信息信息
     *
     * @param id 设备基本信息主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDeviceById(Long id) {
        int rows = deviceMapper.deleteDeviceById(id);
        if (rows > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + id);
        }
        return rows;
    }
}
