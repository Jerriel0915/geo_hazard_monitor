package com.zwei.iot.device.service;

import com.zwei.iot.device.domain.brief.HazardPointBrief;

import java.util.List;

/**
 * 隐患点查询服务 (跨模块接口, 实现在 zwei-iot-hazard)。
 */
public interface IHazardPointQueryService {

    /**
     * 列出所有"监测中" (status=1 AND del_flag='0') 的隐患点摘要。
     */
    List<HazardPointBrief> listMonitoring();

    /**
     * 查询指定分组下的隐患点 ID 列表 (status=1 AND del_flag='0')。
     * @param groupId 分组 ID
     * @return 隐患点 ID 列表
     */
    List<Long> listIdsByGroupId(Long groupId);
}
