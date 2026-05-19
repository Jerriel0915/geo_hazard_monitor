package com.zwei.iot.service.impl;

import com.zwei.iot.domain.VideoDevice;
import com.zwei.iot.mapper.VideoDeviceMapper;
import com.zwei.iot.service.IVideoDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 视频设备Service实现
 *
 * @author zwei
 */
@Service
public class VideoDeviceServiceImpl implements IVideoDeviceService {
    private final VideoDeviceMapper videoDeviceMapper;

    @Autowired
    public VideoDeviceServiceImpl(VideoDeviceMapper videoDeviceMapper) {
        this.videoDeviceMapper = videoDeviceMapper;
    }

    /**
     * 分页查询视频设备
     */
    @Override
    public List<VideoDevice> selectVideoDevicePage(VideoDevice videoDevice, int pageNum, int pageSize) {
        return videoDeviceMapper.selectVideoDeviceList(videoDevice);
    }

    /**
     * 查询所有视频设备
     */
    @Override
    public List<VideoDevice> selectVideoDeviceAll() {
        return videoDeviceMapper.selectVideoDeviceAll();
    }

    /**
     * 根据ID查询视频设备
     */
    @Override
    public VideoDevice selectVideoDeviceById(Long id) {
        return videoDeviceMapper.selectVideoDeviceById(id);
    }

    /**
     * 新增视频设备
     */
    @Override
    @Transactional
    public int insertVideoDevice(VideoDevice videoDevice) {
        return videoDeviceMapper.insertVideoDevice(videoDevice);
    }

    /**
     * 修改视频设备
     */
    @Override
    @Transactional
    public int updateVideoDevice(VideoDevice videoDevice) {
        return videoDeviceMapper.updateVideoDevice(videoDevice);
    }

    /**
     * 删除视频设备（逻辑删除）
     */
    @Override
    @Transactional
    public int deleteVideoDeviceById(Long id) {
        return videoDeviceMapper.deleteVideoDeviceById(id);
    }

    /**
     * 批量删除视频设备
     */
    @Override
    @Transactional
    public int deleteVideoDeviceByIds(Long[] ids) {
        return videoDeviceMapper.deleteVideoDeviceByIds(ids);
    }

    /**
     * 校验设备编号是否唯一
     */
    @Override
    public boolean checkVideoDeviceCodeUnique(VideoDevice videoDevice) {
        VideoDevice result = videoDeviceMapper.checkVideoDeviceCodeUnique(videoDevice.getCode(), videoDevice.getId());
        return result == null;
    }
}