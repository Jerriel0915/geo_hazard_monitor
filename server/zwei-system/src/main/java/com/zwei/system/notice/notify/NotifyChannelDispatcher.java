package com.zwei.system.notice.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知通道分发器。
 * <p>
 * 根据 sys_notify_template.channels 配置的通道列表，遍历所有已注册的
 * {@link INotifyChannel} 实现并依次发送。当前仅内置 in_app 通道，
 * email、sms 通道在对接第三方服务后注入即可自动生效。
 * <p>
 * 设计思路：通过 {@code Map<String, INotifyChannel>} 按名称自动装配，
 * 新增通道只需添加一个 {@code @Component(channelKey)} 实现类，零代码改动。
 */
@Component
public class NotifyChannelDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotifyChannelDispatcher.class);

    private final Map<String, INotifyChannel> channels;

    /**
     * Spring 自动将所有 {@link INotifyChannel} Bean 注入为 Map，
     * key = Bean name (应与 channelKey 一致)。
     */
    public NotifyChannelDispatcher(Map<String, INotifyChannel> channels) {
        this.channels = channels;
    }

    /**
     * 根据通道列表向指定目标发送通知。
     *
     * @param channelKeys 逗号分隔的通道列表（如 "in_app,email,sms"）
     * @param request     发送请求上下文
     * @return 成功发送的通道数
     */
    public int dispatch(String channelKeys, NotifySendRequest request) {
        if (channelKeys == null || channelKeys.isBlank()) {
            channelKeys = "in_app";
        }
        int success = 0;
        for (String key : channelKeys.split(",")) {
            String channelKey = key.trim();
            INotifyChannel channel = channels.get(channelKey);
            if (channel == null) {
                log.warn("未注册的推送通道: {}", channelKey);
                continue;
            }
            try {
                if (channel.send(request)) {
                    success++;
                }
            } catch (Exception e) {
                log.error("通道 {} 发送失败 instanceId={} userId={}", channelKey, request.getInstanceId(), request.getUserId(), e);
            }
        }
        return success;
    }
}
