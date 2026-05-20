package com.zwei.iot.video.service;

import com.zwei.iot.video.domain.VideoDevice;

import java.util.List;

/**
 * 视频设备Service接口
 *
 * @author zwei
 */
public interface IVideoDeviceService {
    /**
     * 分页查询视频设备
     *
     * @param videoDevice 视频设备信息
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @return 视频设备列表
     */
    List<VideoDevice> selectVideoDevicePage(VideoDevice videoDevice, int pageNum, int pageSize);

    /**
     * 查询所有视频设备
     *
     * @return 视频设备列表
     */
    List<VideoDevice> selectVideoDeviceAll();

    /**
     * 根据ID查询视频设备
     *
     * @param id 视频设备ID
     * @return 视频设备
     */
    VideoDevice selectVideoDeviceById(Long id);

    /**
     * 新增视频设备
     *
     * @param videoDevice 视频设备
     * @return 影响行数
     */
    int insertVideoDevice(VideoDevice videoDevice);

    /**
     * 修改视频设备
     *
     * @param videoDevice 视频设备
     * @return 影响行数
     */
    int updateVideoDevice(VideoDevice videoDevice);

    /**
     * 删除视频设备（逻辑删除）
     *
     * @param id 视频设备ID
     * @return 影响行数
     */
    int deleteVideoDeviceById(Long id);

    /**
     * 批量删除视频设备
     *
     * @param ids 需要删除的ID
     * @return 影响行数
     */
    int deleteVideoDeviceByIds(Long[] ids);

    /**
     * 校验设备编号是否唯一
     *
     * @param videoDevice 视频设备
     * @return true-唯一，false-已存在
     */
    boolean checkVideoDeviceCodeUnique(VideoDevice videoDevice);
}