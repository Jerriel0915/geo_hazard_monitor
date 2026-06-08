package com.zwei.iot.broker.component;

import com.zwei.common.utils.StringUtils;
import com.zwei.iot.broker.exception.MqttBusinessException;
import com.zwei.iot.broker.exception.MqttCommunicationException;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
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
 * MQTT 订阅权限校验器。
 *
 * <p>拦截设备侧 SUBSCRIBE 请求，仅允许设备订阅所属传感器对应的主题：
 * {@code sys/v1/{deviceCode}/{sensorNo}/updata}。
 *
 * <h3>校验流程</h3>
 * <ol>
 *   <li>topic 非空 + 前缀 "sys/v1/" 快速过滤</li>
 *   <li>严格正则匹配（字母数字 + _ -，64 字符上限）</li>
 *   <li>数据库存在性校验：按 deviceCode + sensorNo 查 device_sensor 表</li>
 * </ol>
 *
 * <p>注意：主题第二段为 sensor_no（设备内唯一），非 sensor_code（全局唯一）。
 * 查询时按 sensor_no 而非 sensor_code 定位传感器。
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
    // 注意：主题第二段为 sensor_no（设备内唯一），非 sensor_code（全局唯一）
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^sys/v1/(?<deviceCode>[A-Za-z0-9_-]{1,64})/(?<sensorNo>[A-Za-z0-9_-]{1,64})/updata$");

    private final IDeviceSensorService deviceSensorService;
    private final MqttExceptionReporter mqttExceptionReporter;

    @Autowired
    public MqttServerSubscribeValidator(IDeviceSensorService deviceSensorService,
                                        MqttExceptionReporter mqttExceptionReporter) {
        this.deviceSensorService = deviceSensorService;
        this.mqttExceptionReporter = mqttExceptionReporter;
    }

    /**
     * 是否可以订阅
     * 设备只能订阅主题：sys/v1/{deviceCode}/{sensorNo}/updata
     * <p>
     * 主题第二段为 sensor_no（设备内唯一主题编号），非 sensor_code（全局唯一业务标识）。
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

        // 快速判断前缀合法
        if (!StringUtils.startsWith(topicFilter, TOPIC_PREFIX)) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                    "订阅主题前缀非法"
            ));
        }

        // 严格正则匹配
        Matcher matcher = TOPIC_PATTERN.matcher(topicFilter);
        if (!matcher.matches()) {
            return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.InvalidTopic(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS).build(),
                    "订阅主题格式非法"
            ));
        }

        String deviceCode = matcher.group("deviceCode");
        String sensorNo = matcher.group("sensorNo");

        try {
            DeviceSensor sensor = DeviceSensor.builder()
                    .deviceCode(deviceCode)
                    .sensorNo(sensorNo)
                    .build();
            boolean exists = StringUtils.isNotEmpty(deviceSensorService.selectSensorList(sensor));
            if (!exists) {
                return mqttExceptionReporter.rejectWithDebug(new MqttBusinessException.PermissionDenied(
                        mqttExceptionReporter.context(clientId, topicFilter, qoS)
                                .putAttribute("deviceCode", deviceCode)
                                .putAttribute("sensorNo", sensorNo)
                                .build(),
                        "测点不存在或无权限订阅"
                ));
            }
            log.debug("[MQTT] Valid topic. clientId: {}, topic: {}", clientId, topicFilter);
        } catch (Exception e) {
            return mqttExceptionReporter.rejectWithError(new MqttCommunicationException.SubscribeFailed(
                    mqttExceptionReporter.context(clientId, topicFilter, qoS)
                            .putAttribute("deviceCode", deviceCode)
                            .putAttribute("sensorNo", sensorNo)
                            .build(),
                    "订阅校验异常",
                    e
            ), e);
        }

        // 校验通过
        return true;
    }
}
