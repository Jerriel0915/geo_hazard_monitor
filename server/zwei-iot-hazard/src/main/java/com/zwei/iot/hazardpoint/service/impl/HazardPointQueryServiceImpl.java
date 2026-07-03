package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 隐患点查询跨模块服务实现。
 */
@Service
public class HazardPointQueryServiceImpl implements IHazardPointQueryService {

    private final HazardPointMapper hazardPointMapper;

    public HazardPointQueryServiceImpl(HazardPointMapper hazardPointMapper) {
        this.hazardPointMapper = hazardPointMapper;
    }

    @Override
    public List<HazardPointBrief> listMonitoring() {
        return hazardPointMapper.selectByStatusAndDelFlag(HazardPoint.STATUS_MONITORING, "0")
            .stream()
            .map(hp -> new HazardPointBrief(
                hp.getId(), hp.getCode(), hp.getName(),
                hp.getLongitude(), hp.getLatitude()))
            .toList();
    }

    @Override
    public List<Long> listIdsByGroupId(Long groupId) {
        if (groupId == null) {
            return List.of();
        }
        return hazardPointMapper.selectIdsByGroupIdAndStatus(
                groupId, HazardPoint.STATUS_MONITORING, "0");
    }
}
