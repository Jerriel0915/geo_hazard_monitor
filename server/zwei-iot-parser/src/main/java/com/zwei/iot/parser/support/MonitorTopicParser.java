package com.zwei.iot.parser.support;

import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT 监测主题解析器。
 *
 * <p>从监测数据上报主题中提取三要素：协议类型、设备编码、传感器编号。
 * 通过 {@link ITopicPatternService} 动态匹配已注册的协议前缀。
 *
 * <h3>支持的 MQTT 主题格式</h3>
 * <pre>
 * {sourceType}/v1/{deviceCode}/{sensorCode}/updata
 * </pre>
 * <p>其中 {@code sourceType} 来自系统中已启用的解析策略的 {@code source_type} 字段。
 *
 * <p>解析失败时返回 null，由上游 {@link com.zwei.iot.timeseries.service.MonitorIngestFacade} 统一处理。
 */
@Component
public class MonitorTopicParser {

    private final ITopicPatternService topicPatternService;

    @Autowired
    public MonitorTopicParser(ITopicPatternService topicPatternService) {
        this.topicPatternService = topicPatternService;
    }

    /**
     * 解析监测数据主题。
     *
     * @param topic MQTT 主题
     * @return 成功时返回主题对象，失败时返回 {@code null}
     */
    public MonitorTopic parse(String topic) {
        TopicComponents c = topicPatternService.resolveTopic(topic);
        if (c == null) {
            return null;
        }
        return new MonitorTopic(c.sourceType(), c.deviceCode(), c.sensorCode());
    }
}
