package com.zwei.iot.timeseries.support;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT 监测主题解析器。
 *
 * <p>从监测数据上报主题中提取三要素：协议类型、设备编码、传感器编号。
 *
 * <h3>支持的 MQTT 主题格式</h3>
 * <pre>
 * sys/v1/{deviceCode}/{sensorCode}/updata   → 通用 JSON 格式
 * gb/v1/{deviceCode}/{sensorCode}/updata    → 国标字节流格式
 * </pre>
 *
 * <p>解析失败时返回 null，由上游 {@link MonitorIngestFacade} 统一处理。
 */
@Component
public class MonitorTopicParser {
    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^(sys|gb)/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorCode>[A-Za-z0-9_-]{1,100})/updata$");

    /**
     * 解析监测数据主题。
     *
     * @param topic MQTT 主题
     * @return 成功时返回主题对象，失败时返回 {@code null}
     */
    public MonitorTopic parse(String topic) {
        Matcher matcher = TOPIC_PATTERN.matcher(topic == null ? "" : topic);
        if (!matcher.matches()) {
            return null;
        }
        return new MonitorTopic(
                matcher.group(1),
                matcher.group("deviceCode"),
                matcher.group("sensorCode")
        );
    }
}
