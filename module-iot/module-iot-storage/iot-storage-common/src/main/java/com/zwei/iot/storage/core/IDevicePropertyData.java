package com.zwei.iot.storage.core;

import com.zwei.iot.core.thing.domain.DeviceProperty;
import com.zwei.iot.core.thing.domain.DevicePropertyCache;

import java.util.List;
import java.util.Map;

/**
 * 设备属性时序数据接口
 */
public interface IDevicePropertyData {

    /**
     * 按时间范围取设备指定属性的历史数据
     *
     * @param deviceId 设备id
     * @param name     属性名称
     * @param start    开始时间戳
     * @param end      结束时间戳
     * @param size     取时间范围内的数量
     */
    List<DeviceProperty> findDevicePropertyHistory(Long deviceId, String name, long start, long end, int size);

    /**
     * 添加多个属性
     *
     * @param deviceId   设备ID
     * @param properties 属性
     * @param time       属性上报时间
     */
    void addProperties(Long deviceId, Map<String, DevicePropertyCache> properties, long time);

}
