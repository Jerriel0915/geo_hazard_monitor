package com.zwei.iot.core.security;

import org.tio.core.ChannelContext;

/**
 * 设备认证接口
 * 用于设备登录认证和订阅鉴权
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface IDeviceAuthentication {

    /**
     * 设备认证
     *
     * @param channelContext 上下文
     * @param deviceKey 设备id
     * @param deviceSecret 设备密钥
     * @return 是否认证成功
     */
    boolean authenticate(ChannelContext channelContext, String deviceKey, String deviceSecret);

    /**
     * 检查设备是否有权限订阅主题
     * 
     * @param deviceKey 设备id
     * @param topic 订阅主题
     * @return 是否有权限订阅
     */
    boolean checkSubscribePermission(String deviceKey, String topic);

    /**
     * 检查设备是否有权限发布消息
     * 
     * @param deviceKey 设备id
     * @param topic 发布主题
     * @return 是否有权限发布
     */
    boolean checkPublishPermission(String deviceKey, String topic);
}