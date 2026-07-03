package com.zwei.iot.timeseries.service;

import com.zwei.iot.timeseries.config.MonitorIngestProperties;
import com.zwei.iot.timeseries.support.RedisReplyParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream PEL 超时消息回收服务。
 *
 * <p>应用启动时自动回收崩溃恢复后残留的 PEL (Pending Entries List) 消息。
 * 主路径使用 XAUTOCLAIM (Redis 6.2+), 异常时降级为 XPENDING + XCLAIM。
 *
 * <h3>关键不变量</h3>
 * <ul>
 *   <li>先重新入队, 再 XACK + XDEL — 保证崩溃窗口不丢消息</li>
 *   <li>retryCount 重置为 0 — 回收消息获得全新处理机会</li>
 *   <li>单条失败不中断整批 — add/XACK/XDEL 异常仅 skip</li>
 * </ul>
 */
@Slf4j
@Component
public class PendingRecoveryService {

    private final RedisTemplate<Object, Object> redisTemplate;
    private final MonitorIngestProperties properties;

    @Autowired
    public PendingRecoveryService(RedisTemplate<Object, Object> redisTemplate,
                                  MonitorIngestProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 执行 PEL 回收: XAUTOCLAIM 主路径 → 失败降级 XPENDING+XCLAIM。
     *
     * @return 成功回收的消息条数
     */
    public int recover() {
        try {
            return recoverViaXautoclaim();
        } catch (Exception e) {
            log.debug("XAUTOCLAIM 不可用 ({}), 降级使用 XPENDING + XCLAIM",
                    e.getClass().getSimpleName(), e);
            return recoverViaPendingClaim();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 共享工具
    // ═══════════════════════════════════════════════════════════════

    /**
     * 将 Redis fields 数组 (k1, v1, k2, v2, ...) 解码为 Map。
     */
    static Map<String, String> decodeBody(List<Object> fields) {
        Map<String, String> body = new LinkedHashMap<>();
        for (int i = 0; i + 1 < fields.size(); i += 2) {
            body.put(new String((byte[]) fields.get(i), StandardCharsets.UTF_8),
                    fields.get(i + 1) != null
                            ? new String((byte[]) fields.get(i + 1), StandardCharsets.UTF_8)
                            : "");
        }
        body.put("retryCount", "0");
        return body;
    }

    /**
     * 先重新入队, 再 XACK + XDEL 清理原始记录。
     * 顺序不可颠倒: 入队失败则旧记录仍在 PEL, 下次回收重试。
     */
    private void reenqueueAndCleanup(RedisConnection conn, byte[] recordId,
                                      Map<String, String> body,
                                      String streamKey, String consumerGroup) {
        // 1. 重新入队 — 入队失败则不 ACK, 保留 PEL 恢复机会
        redisTemplate.opsForStream().add(
                org.springframework.data.redis.connection.stream.MapRecord
                        .create(streamKey, body));
        // 2. XACK — 确认消费
        conn.execute("XACK",
                serialize(streamKey),
                serialize(consumerGroup),
                recordId);
        // 3. XDEL — 删除旧记录
        conn.execute("XDEL",
                serialize(streamKey),
                recordId);
    }

    static byte[] serialize(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    // ═══════════════════════════════════════════════════════════════
    // 主路径: XAUTOCLAIM
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private int recoverViaXautoclaim() {
        String streamKey = properties.getStreamKey();
        String consumerGroup = properties.getConsumerGroup();
        String consumerName = properties.getConsumerName();
        long minIdleMs = properties.getPelRecoverIdleMs();

        Long recovered = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
            int claimed = 0;
            String startId = "0-0";
            while (true) {
                Object reply = connection.execute("XAUTOCLAIM",
                        serialize(streamKey),
                        serialize(consumerGroup),
                        serialize(consumerName),
                        String.valueOf(minIdleMs).getBytes(StandardCharsets.UTF_8),
                        serialize(startId),
                        serialize("COUNT"),
                        serialize("100"));
                List<Object> result = RedisReplyParser.parseList(reply);
                if (result.isEmpty()) break;
                // [nextStartId, [entries...]]
                Object nextStart = result.get(0);
                startId = nextStart instanceof byte[] ? new String((byte[]) nextStart, StandardCharsets.UTF_8) : "0-0";
                if (result.size() < 2) break;
                Object entriesObj = result.get(1);
                if (!(entriesObj instanceof List<?>)) break;
                List<Object> entries = (List<Object>) entriesObj;
                if (entries.isEmpty()) break;
                for (Object entryObj : entries) {
                    try {
                        if (!(entryObj instanceof List<?> rawEntry)) continue;
                        List<Object> entry = (List<Object>) rawEntry;
                        if (entry.size() < 2) continue;
                        Object idObj = entry.get(0);
                        Object fieldsObj = entry.get(1);
                        if (!(idObj instanceof byte[]) || !(fieldsObj instanceof List<?>)) continue;
                        byte[] recordId = (byte[]) idObj;
                        List<Object> fields = (List<Object>) fieldsObj;
                        Map<String, String> body = decodeBody(fields);
                        String recIdStr = new String(recordId, StandardCharsets.UTF_8);
                        reenqueueAndCleanup(connection, recordId, body, streamKey, consumerGroup);
                        String payloadPreview = body.getOrDefault("payload", "");
                        log.info("PEL 回收: 重新入队 recordId={} payloadPreview={}",
                                recIdStr, payloadPreview.substring(0, Math.min(80, payloadPreview.length())));
                        claimed++;
                    } catch (Exception e) {
                        log.warn("PEL 回收: 单条消息处理失败，跳过", e);
                    }
                }
                if (entries.size() < 100) break;
            }
            return (long) claimed;
        });
        if (recovered != null && recovered > 0) {
            log.info("PEL 回收完成: 共回收 {} 条待确认消息", recovered);
        }
        return recovered != null ? recovered.intValue() : 0;
    }

    // ═══════════════════════════════════════════════════════════════
    // 降级路径: XPENDING + XCLAIM
    // ═══════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private int recoverViaPendingClaim() {
        String streamKey = properties.getStreamKey();
        String consumerGroup = properties.getConsumerGroup();
        String consumerName = properties.getConsumerName();
        long minIdleMs = properties.getPelRecoverIdleMs();

        try {
            redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
                // XPENDING <key> <group> IDLE <min-idle-ms> - + <count>
                Object reply = connection.execute("XPENDING",
                        serialize(streamKey),
                        serialize(consumerGroup),
                        serialize("IDLE"),
                        String.valueOf(minIdleMs).getBytes(StandardCharsets.UTF_8),
                        serialize("-"),
                        serialize("+"),
                        serialize("100"));
                List<Object> pendingEntries = RedisReplyParser.parseList(reply);
                if (pendingEntries.isEmpty()) return null;

                int claimed = 0;
                for (Object entryObj : pendingEntries) {
                    try {
                        if (!(entryObj instanceof List<?> rawEntry)) continue;
                        List<Object> entry = (List<Object>) rawEntry;
                        if (entry.isEmpty()) continue;
                        Object idObj = entry.get(0);
                        if (!(idObj instanceof byte[])) continue;
                        byte[] recordIdBytes = (byte[]) idObj;
                        // XCLAIM <key> <group> <consumer> <min-idle-ms> <id>
                        Object claimReply = connection.execute("XCLAIM",
                                serialize(streamKey),
                                serialize(consumerGroup),
                                serialize(consumerName),
                                String.valueOf(minIdleMs).getBytes(StandardCharsets.UTF_8),
                                recordIdBytes);
                        List<Object> claimedEntries = RedisReplyParser.parseList(claimReply);
                        for (Object ceObj : claimedEntries) {
                            if (!(ceObj instanceof List<?> rawCe)) continue;
                            List<Object> ce = (List<Object>) rawCe;
                            if (ce.size() < 2) continue;
                            Object ceIdObj = ce.get(0);
                            Object ceFieldsObj = ce.get(1);
                            if (!(ceIdObj instanceof byte[]) || !(ceFieldsObj instanceof List<?>)) continue;
                            List<Object> ceFields = (List<Object>) ceFieldsObj;
                            Map<String, String> body = decodeBody(ceFields);
                            reenqueueAndCleanup(connection, (byte[]) ceIdObj, body, streamKey, consumerGroup);
                            claimed++;
                        }
                    } catch (Exception e) {
                        log.warn("PEL 回收(legacy): 单条消息处理失败，跳过", e);
                    }
                }
                if (claimed > 0) {
                    log.info("PEL 回收(legacy)完成: 共回收 {} 条待确认消息", claimed);
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("PEL legacy 回收失败", e);
        }
        return 0;
    }
}
