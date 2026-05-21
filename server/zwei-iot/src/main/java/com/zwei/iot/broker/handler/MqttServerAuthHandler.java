package com.zwei.iot.broker.handler;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.net.core.ChannelContext;
import org.dromara.mica.mqtt.core.server.auth.IMqttServerAuthHandler;
import org.springframework.stereotype.Component;

/**
 *
 * @author Jerriel
 * @date: 2026-05-20
 */
@Component
@Slf4j
public class MqttServerAuthHandler implements IMqttServerAuthHandler {
    /**
     * 认证
     *
     * @param context  ChannelContext
     * @param uniqueId mqtt 内唯一id，默认和 clientId 相同
     * @param clientId 客户端 ID
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    @Override
    public boolean authenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        return false;
    }

    /**
     * 认证
     *
     * @param context  ChannelContext
     * @param uniqueId mqtt 内唯一id，默认和 clientId 相同
     * @param clientId 客户端 ID
     * @param username 用户名
     * @param password 密码
     * @return 是否认证成功
     */
    @Override
    public boolean verifyAuthenticate(ChannelContext context, String uniqueId, String clientId, String username, String password) {
        return IMqttServerAuthHandler.super.verifyAuthenticate(context, uniqueId, clientId, username, password);
    }
}
