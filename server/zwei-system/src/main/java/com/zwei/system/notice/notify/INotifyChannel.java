package com.zwei.system.notice.notify;

/**
 * 通知推送通道接口。
 * <p>
 * 定义统一的通知发送契约，各通道实现负责具体的推送逻辑。
 * 当前支持 in_app（应用内），后续扩展 email、sms 等通道。
 * <p>
 * 使用示例：
 * <pre>{@code
 * @Component("inAppNotifyChannel")
 * public class InAppNotifyChannel implements INotifyChannel {
 *     public String channelKey() { return "in_app"; }
 *     public void send(SysNotifyInstance instance, SysNotifyTarget target) { ... }
 * }
 * }</pre>
 */
public interface INotifyChannel {

    /**
     * 通道标识，对应 sys_notify_template.channels 中的值。
     * Spring Bean 名称应与 {@code channelKey()} 保持一致，
     * 以便 {@code NotifyChannelDispatcher} 通过名称动态获取通道实例。
     */
    String channelKey();

    /**
     * 向指定目标发送通知。
     *
     * @param instance 通知实例（标题 + 内容）
     * @param target   通知目标（用户 + 通道 + 发送状态）
     * @return true 表示发送成功
     */
    boolean send(NotifySendRequest request);

    /**
     * 验证通道是否可用（如邮件服务是否连通、短信 SDK 是否初始化）。
     */
    default boolean isAvailable() {
        return true;
    }
}
