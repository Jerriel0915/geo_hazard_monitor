package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.parser.MonitorPayloadParser;
import com.zwei.iot.timeseries.support.MonitorTopic;
import com.zwei.iot.timeseries.support.MonitorTopicParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MQTT 监测数据接入门面。
 *
 * <p>统一接入门面，负责串联主题解析、元数据解析、报文标准化和缓冲入队流程。
 * 设备标识（deviceId）由上游 MQTT 监听器从已认证会话中提取后传入，避免重复查库。</p>
 */
@Slf4j
@Service
public class MonitorIngestFacade {
    private final MonitorTopicParser topicParser;
    private final MonitorMetadataService metadataService;
    private final MonitorIngestStreamService streamService;
    private final List<MonitorPayloadParser> parsers;

    /**
     * 构造监测数据接入门面。
     *
     * @param topicParser     主题解析器
     * @param metadataService 元数据解析服务
     * @param streamService   Stream 写入服务
     * @param parsers         报文解析器列表
     */
    @Autowired
    public MonitorIngestFacade(MonitorTopicParser topicParser,
                               MonitorMetadataService metadataService,
                               MonitorIngestStreamService streamService,
                               List<MonitorPayloadParser> parsers) {
        this.topicParser = topicParser;
        this.metadataService = metadataService;
        this.streamService = streamService;
        this.parsers = parsers;
    }

    /**
     * 接收并标准化处理 MQTT 监测报文。
     *
     * @param topic    MQTT 主题
     * @param message  报文字节数组
     * @param deviceId 已认证设备主键（由上游 MQTT 监听器从连接上下文中提取）
     * @throws ServiceException 当主题非法、元数据不存在或报文解析失败时抛出
     */
    public void ingest(String topic, byte[] message, Long deviceId) {
        MonitorTopic parsedTopic = topicParser.parse(topic);
        if (parsedTopic == null) {
            throw new ServiceException("监测主题格式非法");
        }
        SensorMetadata metadata = metadataService.requireSensorMetadata(deviceId, parsedTopic.sensorNo());
        MonitorPayloadParser parser = parsers.stream()
                .filter(item -> item.supports(parsedTopic))
                .findFirst()
                .orElseThrow(() -> new ServiceException("未找到可用的报文解析器"));
        List<StandardMeasurementPoint> points = parser.parse(parsedTopic, message, metadata);
        if (points == null || points.isEmpty()) {
            throw new ServiceException("未解析出有效监测点");
        }
        streamService.enqueue(points);
        log.debug("监测报文已入缓冲队列, topic={}, points={}", topic, points.size());
    }
}
