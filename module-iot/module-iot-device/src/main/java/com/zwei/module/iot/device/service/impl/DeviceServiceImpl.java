package com.zwei.module.iot.device.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.framework.manager.CacheWarmupTask;
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.mapper.DeviceMapper;
import com.zwei.module.iot.device.service.IDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 设备基本信息Service业务层处理
 *
 * @author zwei
 * @date 2025-09-05
 */
@Service
@Slf4j
public class DeviceServiceImpl implements IDeviceService, CacheWarmupTask {
    private final DeviceMapper deviceMapper;
    private final RedisCache redisCache;

    private final Random random = new Random();

    private static final String CACHE_KEY_PREFIX = "iot:device:id:";
    private static final Long EXPIRE_SECONDS = 60 * 60 * 12L;

    private IDeviceService deviceService;

    @Autowired
    DeviceServiceImpl(DeviceMapper deviceMapper, RedisCache redisCache) {
        this.deviceMapper = deviceMapper;
        this.redisCache = redisCache;
    }

    @Override
    public String getTaskName() {
        return "DeviceService";
    }

    @Override
    public void warmup() throws InterruptedException {
        int pageSize = 1_000;
        int pageNo = 1;
        long total = 0;

        while (true) {
            PageHelper.startPage(pageNo, pageSize);
            List<Device> devices = selectDeviceList(new Device());

            if (devices == null || devices.isEmpty()) {
                break;
            }

            redisCache.redisTemplate.executePipelined((RedisCallback<?>) connections -> {
                devices.forEach(device -> {
                    byte[] key = (CACHE_KEY_PREFIX + device.getId()).getBytes();
                    byte[] value = JSONObject.toJSONString(device).getBytes();
                    long expire = EXPIRE_SECONDS + random.nextLong() % 7200;

                    connections.setEx(key, expire, value);
                });
                return null;
            });

            total += devices.size();
            pageNo++;

            if (pageNo % 10 == 0) {
                Thread.sleep(100);  // 每1万条休息100ms
            }
        }

        log.info("设备缓存预热结束，总量: {}", total);
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
        log.info("设备新增, device: {}", device);
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
        log.info("设备更新, device: {}", device);

        int rows = deviceMapper.updateDevice(device);
        if (rows > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + device.getId());
            log.info("设备更新成功，相关缓存条目已删除");
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
        log.info("设备删除, ids: {}", ids);

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
        log.info("设备删除, id: {}", id);

        int rows = deviceMapper.deleteDeviceById(id);
        if (rows > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + id);
        }
        return rows;
    }
}
