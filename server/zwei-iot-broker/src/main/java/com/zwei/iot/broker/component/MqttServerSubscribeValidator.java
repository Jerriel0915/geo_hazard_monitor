package com.zwei.iot.broker.component;

import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.exception.MqttBusinessException;
import com.zwei.iot.broker.exception.MqttCommunicationException;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.model.MqttDeviceSession;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.service.IDeviceSensorService;
import com.zwei.iot.device.service.ITopicPatternService;
import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerSubscribeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * MQTT 订阅权限校验器。
 *
 * <p>拦截设备侧 SUBSCRIBE 请求，仅允许设备订阅所属传感器对应的主题。
 * 主题格式由 {@link ITopicPatternService} 动态校验，支持已注册的全部协议前缀。
 *
 * <h3>校验流程</h3>
 * <ol>
 *   <li>topic 非空</li>
 *   <li>ITopicPatternService 解析主题模板（匹配前缀 + 格式校验）</li>
 *   <li>设备归属校验：验证订阅客户端的 deviceCode 与主题 deviceCode 一致</li>
 *   <li>数据库存在性校验：按 deviceCode + sensorCode 查 device_sensor 表</li>
 * </ol>
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Component
@Slf4j
public class MqttServerSubscribeValidator implements IMqttServerSubscribeValidator {
    private final IDeviceSensorService deviceSensorService;
    private final MqttExceptionReporter mqttExceptionReporter;
    private final MqttDeviceSessionRegistry sessionRegistry;
    private final ITopicPatternService topicPatternService;

    @Autowired
    public MqttServerSubscribeValidator(IDeviceSensorService deviceSensorService,
                                        MqttExceptionReporter mqttExceptionReporter,
                                        MqttDeviceSessionRegistry sessionRegistry,
                                        ITopicPatternService topicPatternService) {
        this.deviceSensorService = deviceSensorService;
        this.mqttExceptionReporter = mqttExceptionReporter;
        this.sessionRegistry = sessionRegistry;
        this.topicPatternService = topicPatternService;
    }

    /**
     * 是否可以订阅。
     * 通过 {@link ITopicPatternService#resolveTopic} 动态校验主题格式及协议前缀。
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
        if (StringUtils.isBlank(topicFilter)) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                    "订阅主题为空"
            ));
        }

        TopicComponents c = topicPatternService.resolveTopic(topicFilter);
        if (c == null) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                    "订阅主题格式非法或前缀不匹配"
            ));
        }

        String deviceCode = c.deviceCode();
        String sensorCode = c.sensorCode();

        // 验证订阅客户端是否归属此 deviceCode（设备间数据隔离）
        String normalizedClientId = clientId == null ? null : clientId.trim();
        Optional<MqttDeviceSession> session = sessionRegistry.getByClientId(normalizedClientId);
        if (session.isEmpty()) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS)
                            .putAttribute("deviceCode", deviceCode)
                            .build(),
                    "未建立鉴权会话，禁止订阅"
            ));
        }
        if (!Objects.equals(session.get().deviceCode(), deviceCode)) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS)
                            .putAttribute("deviceCode", deviceCode)
                            .putAttribute("sessionDeviceCode", session.get().deviceCode())
                            .build(),
                    "设备与订阅主题不匹配，禁止订阅"
            ));
        }

        try {
            DeviceSensor sensor = DeviceSensor.builder()
                    .deviceCode(deviceCode)
                    .sensorCode(sensorCode)
                    .build();
            boolean exists = StringUtils.isNotEmpty(deviceSensorService.selectSensorList(sensor));
            if (!exists) {
                return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                        mqttExceptionReporter.context(clientId, topicFilter, qoS)
                                .putAttribute("deviceCode", deviceCode)
                                .putAttribute("sensorCode", sensorCode)
                                .build(),
                        "测点不存在或无权限订阅"
                ));
            }
            log.debug("[MQTT] Valid topic. clientId: {}, topic: {}", clientId, topicFilter);
        } catch (Exception e) {
            return mqttExceptionReporter.rejectWithError(new MqttCommunicationException.SubscribeFailed(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS)
                            .putAttribute("deviceCode", deviceCode)
                            .putAttribute("sensorCode", sensorCode)
                            .build(),
                    "订阅校验异常",
                    e
            ), e);
        }

        return true;
    }
}
