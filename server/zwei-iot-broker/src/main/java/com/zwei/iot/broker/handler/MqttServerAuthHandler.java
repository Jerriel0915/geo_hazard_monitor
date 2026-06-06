package com.zwei.iot.broker.handler;

import com.zwei.iot.broker.exception.MqttConnectionException;
import com.zwei.iot.broker.exception.MqttErrorContext;
import com.zwei.iot.broker.exception.MqttExceptionReporter;
import com.zwei.iot.broker.service.MqttDeviceAuthService;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.springframework.stereotype.Component;

/**
 * MQTT 连接鉴权入口。
 * <p>
 * 该处理器只负责承接 Broker 的认证回调，真正的账号校验、失败封禁、
 * 单设备单连接控制与审计日志记录统一下沉到 {@link MqttDeviceAuthService}，
 * 避免协议层回调中混入过多业务判断。
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Component
@Slf4j
public class MqttServerAuthHandler implements IMqttServerAuthHandler {
    private final MqttDeviceAuthService mqttDeviceAuthService;
    private final MqttExceptionReporter mqttExceptionReporter;

    public MqttServerAuthHandler(MqttDeviceAuthService mqttDeviceAuthService,
                                 MqttExceptionReporter mqttExceptionReporter) {
        this.mqttDeviceAuthService = mqttDeviceAuthService;
        this.mqttExceptionReporter = mqttExceptionReporter;
    }

    /**
     * 执行 MQTT CONNECT 鉴权。
     *
     * @param context  当前连接上下文，用于回填设备标识与来源 IP
     * @param uniqueId mqtt 内唯一 ID，通常与 clientId 一致
     * @param clientId 客户端 ID
     * @param username 设备认证账号
     * @param password 设备认证密码
     * @return {@code true} 表示通过设备身份校验并完成会话注册，{@code false} 表示拒绝接入
     */
    @Override
    public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        try {
            return mqttDeviceAuthService.authenticate(context, uniqueId, clientId, username, password);
        } catch (RuntimeException e) {
            return mqttExceptionReporter.rejectWithError(new MqttConnectionException.BrokerUnavailable(
                    mqttExceptionReporter.context(clientId)
                            .putAttribute("username", username)
                            .build(),
                    "鉴权处理异常",
                    e
            ), e);
        }
    }

    /**
     * 复用框架默认的二次校验流程。
     *
     * @param context  当前连接上下文
     * @param uniqueId mqtt 内唯一 ID，通常与 clientId 一致
     * @param clientId 客户端 ID
     * @param username 设备认证账号
     * @param password 设备认证密码
     * @return 框架默认校验结果
     */
    @Override
    public boolean verifyAuthenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        return IMqttServerAuthHandler.super.verifyAuthenticate(context, uniqueId, clientId, username, password);
    }
}
