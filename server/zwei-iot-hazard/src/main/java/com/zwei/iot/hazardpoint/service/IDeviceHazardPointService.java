package com.zwei.iot.hazardpoint.service;

import com.zwei.iot.hazardpoint.domain.dto.BindDeviceRequest;
import com.zwei.iot.hazardpoint.domain.dto.BoundDeviceVO;
import com.zwei.iot.hazardpoint.domain.dto.UnboundDeviceVO;

import java.util.List;

/**
 * 设备隐患点关联Service接口
 *
 * @author zwei
 */
public interface IDeviceHazardPointService {

    /**
     * 获取隐患点已绑定的设备列表
     *
     * @param hazardPointId 隐患点ID
     * @return 已绑定设备列表
     */
    List<BoundDeviceVO> getBoundDevices(Long hazardPointId);

    /**
     * 获取未绑定设备列表
     *
     * @param hazardPointId 隐患点ID
     * @param keyword        关键词（设备/传感器名称模糊查询）
     * @return 未绑定设备列表
     */
    List<UnboundDeviceVO> getUnboundDevices(Long hazardPointId, String keyword);

    /**
     * 绑定设备到隐患点
     *
     * @param hazardPointId 隐患点ID
     * @param request       绑定请求参数
     * @param username      操作人用户名
     * @return 影响行数
     */
    int bindDevices(Long hazardPointId, BindDeviceRequest request, String username);

    /**
     * 解绑设备
     *
     * @param hazardPointId 隐患点ID
     * @param deviceIds     设备ID列表
     * @return 影响行数
     */
    int unbindDevices(Long hazardPointId, List<Long> deviceIds);
}
