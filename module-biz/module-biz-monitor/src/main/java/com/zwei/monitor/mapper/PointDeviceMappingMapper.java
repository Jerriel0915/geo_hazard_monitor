package com.zwei.monitor.mapper;

import java.util.List;

import com.zwei.monitor.domain.PointDevice;

/**
 * 监测点位(测站点)Mapper接口
 * 
 * @author zwei
 * @date 2025-10-15
 */
public interface PointDeviceMappingMapper 
{

    public void deleteByPointIds(Long[]  id);

    public void deleteByPointId(Long id);

    /**
     * 根据监测点位ID查询关联的设备映射
     * 
     * @param pointId 监测点位ID
     * @return 关联的设备映射列表
     */
    public List<PointDevice> selectByPointId(Long pointId);
}