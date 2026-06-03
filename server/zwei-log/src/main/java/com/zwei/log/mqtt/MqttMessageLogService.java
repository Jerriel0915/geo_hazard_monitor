package com.zwei.log.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * MQTT 数据日志服务。
 * <p>
 * 使用内存环形缓冲区记录最近通过的设备监测消息元数据，
 * 供管理后台实时查看消息流量和排查异常。
 * <p>
 * 为避免内存溢出，缓冲区最多保留 {@link #MAX_CAPACITY} 条记录，
 * 达到上限后自动淘汰最早写入的条目。
 */
@Slf4j
@Service
public class MqttMessageLogService {

    /**
     * 内存缓冲区最大容量
     */
    private static final int MAX_CAPACITY = 1000;
    /**
     * 负载内容截断长度（字符）
     */
    private static final int PAYLOAD_TRUNCATE_CHARS = 500;

    private final ConcurrentLinkedDeque<MqttMessageLog> buffer = new ConcurrentLinkedDeque<>();

    /**
     * 记录一条消息日志。
     *
     * @param clientId MQTT clientId
     * @param username 设备认证账号
     * @param topic    发布主题
     * @param payload  原始消息负载字节数组
     */
    public void record(String clientId, String username, String topic, byte[] payload) {
        MqttMessageLog entry = new MqttMessageLog();
        entry.setReceiveTime(System.currentTimeMillis());
        entry.setClientId(clientId);
        entry.setUsername(username);
        entry.setTopic(topic);
        entry.setPayloadSize(payload != null ? payload.length : 0);
        if (payload != null && payload.length > 0) {
            String raw = new String(payload, StandardCharsets.UTF_8);
            entry.setPayload(raw.length() > PAYLOAD_TRUNCATE_CHARS
                    ? raw.substring(0, PAYLOAD_TRUNCATE_CHARS) + "..."
                    : raw);
        }
        buffer.offerLast(entry);
        while (buffer.size() > MAX_CAPACITY) {
            buffer.pollFirst();
        }
    }

    /**
     * 分页查询消息日志（按接收时间倒序，最新在前）。
     *
     * @param page     页码（从 1 开始）
     * @param pageSize 每页大小
     * @param clientId 可选过滤：clientId 模糊匹配，传空则不筛选
     * @param topic    可选过滤：topic 模糊匹配，传空则不筛选
     * @return 分页结果
     */
    public PageResult query(int page, int pageSize, String clientId, String topic) {
        List<MqttMessageLog> all = new ArrayList<>(buffer);
        Collections.reverse(all);

        if (clientId != null && !clientId.isEmpty()) {
            all = all.stream()
                    .filter(e -> e.getClientId() != null && e.getClientId().contains(clientId))
                    .collect(Collectors.toList());
        }
        if (topic != null && !topic.isEmpty()) {
            all = all.stream()
                    .filter(e -> e.getTopic() != null && e.getTopic().contains(topic))
                    .collect(Collectors.toList());
        }

        int total = all.size();
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= total) {
            return new PageResult(page, pageSize, total, Collections.emptyList());
        }
        int toIndex = Math.min(fromIndex + pageSize, total);
        return new PageResult(page, pageSize, total, all.subList(fromIndex, toIndex));
    }

    public static class PageResult {
        public int pageNumber;
        public int pageSize;
        public int totalRow;
        public List<MqttMessageLog> list;

        public PageResult(int pageNumber, int pageSize, int totalRow, List<MqttMessageLog> list) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalRow = totalRow;
            this.list = list;
        }
    }
}
