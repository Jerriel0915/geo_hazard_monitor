package com.zwei.monitor.domain;

import lombok.Builder;
import lombok.Data;

/**
 * MQTT 服务器运行配置摘要。
 */
@Data
@Builder
public class MqttConfigInfo {
    /**
     * 心跳超时（毫秒）
     */
    private long heartbeatTimeout;
    /**
     * 接收缓冲区大小
     */
    private String readBufferSize;
    /**
     * 消息最大字节数
     */
    private String maxBytesInMessage;
    /**
     * 是否开启 MQTT 鉴权
     */
    private boolean authEnabled;
    /**
     * 调试模式
     */
    private boolean debug;
    /**
     * 统计指标收集
     */
    private boolean statEnable;
}
