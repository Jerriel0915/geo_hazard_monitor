package com.zwei.iot.timeseries.parser;

import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.service.MonitorIngestFacade;
import com.zwei.iot.timeseries.support.MonitorTopic;

import java.util.List;

/**
 * MQTT 监测数据解析器。
 *
 * <p>统一解析器接口，按主题协议类型分发通用报文（sys）与国标报文（gb）解析实现。
 * 实现类由 Spring 自动注入为集合，由 {@link MonitorIngestFacade} 根据主题类型动态选择。</p>
 */
public interface MonitorPayloadParser {
    /**
     * 判断当前解析器是否支持指定主题。
     *
     * @param topic 监测主题信息
     * @return 支持时返回 {@code true}
     */
    boolean supports(MonitorTopic topic);

    /**
     * 将原始 MQTT 报文解析为标准化时序点集合。
     *
     * @param topic    监测主题信息
     * @param message  原始报文字节数组
     * @param metadata 传感器元数据
     * @return 标准化时序点集合
     */
    List<StandardMeasurementPoint> parse(MonitorTopic topic, byte[] message, SensorMetadata metadata);
}
