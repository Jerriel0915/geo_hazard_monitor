package com.zwei.iot.hazardpoint.service;

import com.zwei.iot.hazardpoint.domain.dto.BindVideoDeviceRequest;
import com.zwei.iot.video.domain.BoundVideoDeviceVO;

import java.util.List;

/**
 * 视频设备隐患点关联Service接口
 */
public interface IVideoDeviceHazardPointService {

    /**
     * 获取隐患点已绑定的视频设备列表
     *
     * @param hazardPointId 隐患点ID
     * @return 已绑定视频设备列表
     */
    List<BoundVideoDeviceVO> getBoundVideoDevices(Long hazardPointId);

    /**
     * 绑定视频设备到隐患点
     *
     * @param hazardPointId 隐患点ID
     * @param request 绑定请求
     * @param username 操作人
     * @return 影响行数
     */
    int bindVideoDevices(Long hazardPointId, BindVideoDeviceRequest request, String username);

    /**
     * 解绑视频设备
     *
     * @param hazardPointId 隐患点ID
     * @param videoDeviceIds 视频设备ID列表
     * @return 影响行数
     */
    int unbindVideoDevices(Long hazardPointId, List<Long> videoDeviceIds);
}
