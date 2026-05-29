package com.zwei.iot.timeseries.support;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 监测数据主题解析。
 *
 * <p>新增正式监测主题解析规则，用于从 MQTT 主题中提取来源类型、设备ID和传感器编号。</p>
 */
@Component
public class MonitorTopicParser {
    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^(sys|gb)/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");

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
                matcher.group("sensorNo")
        );
    }
}
