package com.zwei.iot.hazardpoint.service.impl;

import com.zwei.common.constant.IotConstants;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.device.domain.brief.DeviceBrief;
import com.zwei.iot.device.service.IAlarmQueryService;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.mapper.DeviceHazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointMapper;
import com.zwei.iot.hazardpoint.mapper.HazardPointGroupMapper;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 隐患点Service实现
 *
 * @author zwei
 */
@Service
public class HazardPointServiceImpl implements IHazardPointService {
    private final HazardPointMapper hazardPointMapper;
    private final HazardPointGroupMapper hazardPointGroupMapper;
    private final DeviceHazardPointMapper deviceHazardPointMapper;
    private final IAlarmQueryService alarmQueryService;

    @Autowired
    public HazardPointServiceImpl(HazardPointMapper hazardPointMapper,
                                  HazardPointGroupMapper hazardPointGroupMapper,
                                  DeviceHazardPointMapper deviceHazardPointMapper,
                                  IAlarmQueryService alarmQueryService) {
        this.hazardPointMapper = hazardPointMapper;
        this.hazardPointGroupMapper = hazardPointGroupMapper;
        this.deviceHazardPointMapper = deviceHazardPointMapper;
        this.alarmQueryService = alarmQueryService;
    }

    /**
     * 根据条件分页查询隐患点列表
     *
     * @param hazardPoint 隐患点信息
     * @return 隐患点集合
     */
    @Override
    public List<HazardPoint> selectHazardPointList(HazardPoint hazardPoint) {
        return hazardPointMapper.selectHazardPointList(hazardPoint);
    }

    /**
     * 根据ID查询隐患点
     *
     * @param id 隐患点ID
     * @return 隐患点信息
     */
    @Override
    @Cacheable(value = "hazardPoint", key = "#id")
    public HazardPoint selectHazardPointById(Long id) {
        return hazardPointMapper.selectHazardPointById(id);
    }

    /**
     * 新增隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#hazardPoint.id")
    public int insertHazardPoint(HazardPoint hazardPoint) {
        if (!checkHazardPointCodeUnique(hazardPoint.getCode())) {
            throw new ServiceException("隐患点编号已存在");
        }
        if (hazardPoint.getStatus() == null) {
            hazardPoint.setStatus(IotConstants.HAZARD_POINT_STATUS_MONITORING);
        }
        validateGroupId(hazardPoint.getGroupId());
        return hazardPointMapper.insertHazardPoint(hazardPoint);
    }

    /**
     * 修改隐患点
     *
     * @param hazardPoint 隐患点信息
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#hazardPoint.id")
    public int updateHazardPoint(HazardPoint hazardPoint) {
        HazardPoint existing = hazardPointMapper.selectHazardPointById(hazardPoint.getId());
        if (existing == null) {
            throw new ServiceException("隐患点不存在");
        }
        validateGroupId(hazardPoint.getGroupId());
        return hazardPointMapper.updateHazardPoint(hazardPoint);
    }

    /**
     * 删除隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#id")
    public int deleteHazardPointById(Long id) {
        return hazardPointMapper.deleteHazardPointById(id);
    }

    /**
     * 批量删除隐患点
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", allEntries = true)
    public int deleteHazardPointByIds(Long[] ids) {
        return hazardPointMapper.deleteHazardPointByIds(ids);
    }

    /**
     * 校验隐患点编号是否唯一
     *
     * @param code 隐患点编号
     * @return 结果
     */
    @Override
    public boolean checkHazardPointCodeUnique(String code) {
        if (!StringUtils.hasText(code)) {
            return true;
        }
        HazardPoint existing = hazardPointMapper.checkHazardPointCodeUnique(code);
        return existing == null;
    }

    /**
     * 停测/恢复隐患点
     *
     * @param id 隐患点ID
     * @param pause true-停测, false-恢复
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#id")
    public int updateHazardPointPause(Long id, boolean pause) {
        Integer newStatus = pause ? IotConstants.HAZARD_POINT_STATUS_PAUSED : IotConstants.HAZARD_POINT_STATUS_MONITORING;
        return hazardPointMapper.updateHazardPointStatus(id, newStatus);
    }

    /**
     * 完结隐患点
     *
     * @param id 隐患点ID
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", key = "#id")
    public int completeHazardPoint(Long id) {
        return hazardPointMapper.updateHazardPointStatus(id, IotConstants.HAZARD_POINT_STATUS_COMPLETED);
    }

    /**
     * 批量操作隐患点(停测/恢复/完结)
     *
     * @param ids 隐患点ID数组
     * @param operation 操作类型: pause/resume/complete
     * @return 结果
     */
    @Override
    @CacheEvict(value = "hazardPoint", allEntries = true)
    public int batchOperateHazardPoint(Long[] ids, String operation) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("请选择要操作的隐患点");
        }

        Integer newStatus;
        if (IotConstants.OPERATION_PAUSE.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_PAUSED;
        } else if (IotConstants.OPERATION_RESUME.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_MONITORING;
        } else if (IotConstants.OPERATION_COMPLETE.equals(operation)) {
            newStatus = IotConstants.HAZARD_POINT_STATUS_COMPLETED;
        } else {
            throw new ServiceException("无效的操作类型");
        }

        return hazardPointMapper.batchUpdateHazardPointStatus(Arrays.asList(ids), newStatus);
    }

    @Override
    public List<Map<String, Object>> getMonitorRates(int windowMinutes) {
        return deviceHazardPointMapper.selectMonitorRateByHazardPoint(windowMinutes);
    }

    @Override
    public List<Map<String, Object>> getMapOverview() {
        // 查询所有活跃隐患点 (status=1 监测中, delFlag=0)
        HazardPoint filter = new HazardPoint();
        filter.setStatus(1);
        List<HazardPoint> hps = hazardPointMapper.selectHazardPointList(filter);

        // 批量查询告警状态
        List<Long> hpIds = hps.stream().map(HazardPoint::getId).collect(Collectors.toList());
        Map<Long, Boolean> alarmMap = alarmQueryService.hasPendingAlarm(hpIds);

        // 批量查询设备列表
        Map<Long, List<DeviceBrief>> deviceMap = new HashMap<>();
        for (Long hpId : hpIds) {
            deviceMap.put(hpId, deviceHazardPointMapper.selectDeviceBriefByHazardPoint(hpId));
        }

        // 组装结果
        List<Map<String, Object>> result = new ArrayList<>(hps.size());
        for (HazardPoint hp : hps) {
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("id", hp.getId());
            vo.put("name", hp.getName());
            vo.put("code", hp.getCode());
            vo.put("type", hp.getGroupName());
            vo.put("description", hp.getDescription());
            vo.put("longitude", hp.getLongitude());
            vo.put("latitude", hp.getLatitude());
            vo.put("status", hp.getStatus());
            vo.put("hasAlarm", alarmMap.getOrDefault(hp.getId(), false));
            vo.put("deviceCount", hp.getDeviceCount());

            List<DeviceBrief> devices = deviceMap.getOrDefault(hp.getId(), Collections.emptyList());
            List<Map<String, String>> deviceItems = new ArrayList<>(devices.size());
            for (DeviceBrief d : devices) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("name", d.name());
                String status = java.util.Objects.equals(1, d.onlineStatus()) ? "online" : "offline";
                item.put("status", status);
                deviceItems.add(item);
            }
            vo.put("devices", deviceItems);

            // 等级推导: 有告警→high, 有设备→medium, 其他→low
            String level = "low";
            if (Boolean.TRUE.equals(alarmMap.getOrDefault(hp.getId(), false))) {
                level = "high";
            } else if (hp.getDeviceCount() != null && hp.getDeviceCount() > 0) {
                level = "medium";
            }
            vo.put("level", level);

            result.add(vo);
        }
        return result;
    }

    private void validateGroupId(Long groupId) {
        if (groupId == null) {
            return;
        }
        HazardPointGroup group = hazardPointGroupMapper.selectHazardPointGroupById(groupId);
        if (group == null) {
            throw new ServiceException("隐患点分组不存在");
        }
    }
}
