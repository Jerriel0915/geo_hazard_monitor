package com.zwei.iot.broker.component;

import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.service.IDeviceSensorService;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Component
@Slf4j
public class MqttServerSubscribeValidator implements IMqttServerSubscribeValidator {
    // 统一前缀，快速判断
    private static final String TOPIC_PREFIX = "sys/v1/";
    // 严格正则，只允许出现字母数字和特殊符号'_' '-'，且主题必须以 /updata 结尾
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^sys/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorCode>[A-Za-z0-9_-]{1,64})/updata$");

    private final IDeviceSensorService deviceSensorService;

    @Autowired
    public MqttServerSubscribeValidator(IDeviceSensorService deviceSensorService) {
        this.deviceSensorService = deviceSensorService;
    }

    /**
     * 是否可以订阅
     * 设备只能订阅主题：sys/v1/{deviceCode}/{sensorCode}/updata
     *
     * @param context     ChannelContext
     * @param clientId    客户端 id
     * @param topicFilter 订阅 topic
     * @param qoS         MqttQoS
     * @return 是否可以订阅
     */
    @Override
    public boolean isValid(ChannelContext context, String clientId, String topicFilter, MqttQoS qoS) {
        // 空topic过滤
        if (topicFilter == null || topicFilter.isBlank()) {
            log.debug("[MQTT] Empty topic. clientId: {}, topic: {}", clientId, topicFilter);
            return false;
        }

        // 快速判断前缀合法
        if (!topicFilter.startsWith(TOPIC_PREFIX)) {
            log.debug("[MQTT] Invalid topic prefix. clientId: {}, topic: {}", clientId, topicFilter);
            return false;
        }

        // 严格正则匹配
        Matcher matcher = TOPIC_PATTERN.matcher(topicFilter);
        if (!matcher.matches()) {
            log.debug("[MQTT] Invalid topic format. clientId: {}, topic: {}", clientId, topicFilter);
            return false;
        }

        String deviceCode = matcher.group("deviceCode");
        String sensorCode = matcher.group("sensorCode");

        try {
            DeviceSensor sensor = DeviceSensor.builder()
                    .deviceCode(deviceCode)
                    .sensorCode(sensorCode)
                    .build();
            boolean exists = deviceSensorService.selectSensorList(sensor).stream()
                    .findFirst()
                    .isPresent();
            if (!exists) {
                log.debug("[MQTT] Sensor not found. clientId: {}, deviceCode: {}, sensorCode: {}", clientId, deviceCode, sensorCode);
                return false;
            }
            log.debug("[MQTT] Valid topic. clientId: {}, topic: {}", clientId, topicFilter);
        } catch (Exception e) {
            log.error("[MQTT] Exception while validating topic. clientId: {}, topic: {}, deviceCode: {}, sensorCode: {}", clientId, topicFilter, deviceCode, sensorCode, e);
            return false;
        }

        // 校验通过
        return true;
    }
}
