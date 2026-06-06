package com.zwei.iot.video.service.impl;

import com.zwei.iot.device.service.IVideoDeviceStatService;
import com.zwei.iot.video.mapper.VideoDeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VideoDeviceStatServiceImpl implements IVideoDeviceStatService {
    private final VideoDeviceMapper videoDeviceMapper;

    @Autowired
    public VideoDeviceStatServiceImpl(VideoDeviceMapper videoDeviceMapper) {
        this.videoDeviceMapper = videoDeviceMapper;
    }

    @Override public int countAll() { return videoDeviceMapper.countAll(); }
    @Override public List<Map<String, Object>> countByStatus() { return videoDeviceMapper.countByStatus(); }
}
