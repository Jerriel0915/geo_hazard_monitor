package com.zwei.monitor.domain;

import lombok.Data;

/**
 * MQTT 服务器统计信息。
 * <p>
 * 映射 mica-mqtt HTTP API {@code GET /api/v1/stats} 的嵌套响应结构：
 * <pre>{@code
 * { "data": { "connections": {...}, "messages": {...}, "nodes": {...} }, "code": 1 }
 * }</pre>
 * 当 upstream 字段为 {@code false} 时，说明无法从 mica-mqtt 获取实时数据。
 */
@Data
public class MqttStatsResponse {
    /**
     * 是否成功从 mica-mqtt HTTP API 获取数据
     */
    private boolean upstream;
    /**
     * 连接统计
     */
    private Connections connections;
    /**
     * 消息统计
     */
    private Messages messages;
    /**
     * 节点统计
     */
    private Nodes nodes;
    /**
     * 服务启动时间戳（毫秒）
     */
    private Long startTime;

    // ---- 便捷透传字段，供前端直接读取 ----

    public long getConnectionsSize() {
        return connections != null ? connections.getSize() : 0;
    }

    public long getConnectionsAccepted() {
        return connections != null ? connections.getAccepted() : 0;
    }

    public long getConnectionsClosed() {
        return connections != null ? connections.getClosed() : 0;
    }

    public long getMessagesHandledPackets() {
        return messages != null ? messages.getHandledPackets() : 0;
    }

    public long getMessagesHandledBytes() {
        return messages != null ? messages.getHandledBytes() : 0;
    }

    public long getMessagesReceivedPackets() {
        return messages != null ? messages.getReceivedPackets() : 0;
    }

    public long getMessagesReceivedBytes() {
        return messages != null ? messages.getReceivedBytes() : 0;
    }

    public long getMessagesSendPackets() {
        return messages != null ? messages.getSendPackets() : 0;
    }

    public long getMessagesSendBytes() {
        return messages != null ? messages.getSendBytes() : 0;
    }

    // ---- 内嵌结构 ----

    @Data
    public static class Connections {
        private long accepted;
        private long closed;
        private long size;
    }

    @Data
    public static class Messages {
        private long handledPackets;
        private long handledBytes;
        private long receivedPackets;
        private long receivedBytes;
        private long sendPackets;
        private long sendBytes;
        /**
         * TCP 接收字节速率（messages.bytesPerTcpReceive），mica-mqtt 返回浮点数
         */
        private double bytesPerTcpReceive;
        /**
         * TCP 接收包速率（messages.packetsPerTcpReceive）
         */
        private double packetsPerTcpReceive;
    }

    @Data
    public static class Nodes {
        /**
         * 客户端节点数
         */
        private int clientNodes;
        /**
         * 连接数（与 connections.size 一致）
         */
        private int connections;
        /**
         * 用户数
         */
        private int users;
    }

    public static MqttStatsResponse unavailable() {
        MqttStatsResponse r = new MqttStatsResponse();
        r.setUpstream(false);
        return r;
    }
}
