package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.iot.device.domain.brief.HazardPointBrief;
import com.zwei.iot.device.service.IHazardPointQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
        List<HazardPoint> all = hazardPointMapper.selectAll();
        return all.stream()
            .filter(hp -> hp.getStatus() != null && hp.getStatus() == 1)
            .filter(hp -> hp.getDelFlag() == null || "0".equals(hp.getDelFlag()))
            .map(hp -> new HazardPointBrief(
                hp.getId(), hp.getCode(), hp.getName(),
                hp.getLongitude(), hp.getLatitude()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> listIdsByGroupId(Long groupId) {
        return hazardPointMapper.selectAll().stream()
            .filter(hp -> groupId.equals(hp.getGroupId()))
            .filter(hp -> hp.getStatus() != null && hp.getStatus() == 1)
            .filter(hp -> hp.getDelFlag() == null || "0".equals(hp.getDelFlag()))
            .map(HazardPoint::getId)
            .collect(Collectors.toList());
    }
}
