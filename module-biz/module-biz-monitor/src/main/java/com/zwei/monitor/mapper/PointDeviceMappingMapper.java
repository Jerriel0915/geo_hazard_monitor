package com.zwei.monitor.mapper;

import com.zwei.monitor.domain.PointDevice;

import java.util.List;

/**
 * 监测点位(测站点)Mapper接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface PointDeviceMappingMapper 
{

    void deleteByPointIds(Long[] id);

    void deleteByPointId(Long id);

    /**
     * 根据监测点位ID查询关联的设备映射
     * 
     * @param pointId 监测点位ID
     * @return 关联的设备映射列表
     */
    List<PointDevice> selectByPointId(Long pointId);
}