package com.zwei.log.mqtt.exception.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zwei.common.event.MqttMessageRejectEvent;
import com.zwei.log.infrastructure.persistence.mysql.ExceptionLogMapper;
import com.zwei.log.mqtt.exception.domain.ExceptionMessageLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 异常报文日志服务。
 * <p>
 * 通过 Spring 事件机制异步消费 {@link MqttMessageRejectEvent}，
 * 将已认证但解析/报送失败的报文持久化到 mqtt_exception_log 表，
 * 供"服务状态 → 异常报文"子页查询与导出。
 * <p>
 * 与 {@code MqttMessageLogService}（内存型数据日志）不同，本服务为 DB 持久化，
 * 支持时间范围保留与定时清理。
 *
 * @author zwei
 */
@Slf4j
@Service
public class ExceptionMessageLogService {

    private static final int PAYLOAD_TRUNCATE_CHARS = 500;
    private static final int REASON_TRUNCATE_CHARS = 500;
    private static final int STACK_TRUNCATE_CHARS = 2000;

    private final ExceptionLogMapper exceptionLogMapper;

    public ExceptionMessageLogService(ExceptionLogMapper exceptionLogMapper) {
        this.exceptionLogMapper = exceptionLogMapper;
    }

    /**
     * Spring 事件监听：异步接收并持久化异常报文事件。
     * <p>
     * 使用 {@code @Async} 在线程池中执行 DB 写入，避免阻塞 MQTT 消息监听线程。
     * 线程池满时回退为调用方线程执行（CallerRunsPolicy），不会丢失事件。
     */
    @Async
    @EventListener
    public void onMqttMessageReject(MqttMessageRejectEvent event) {
        try {
            exceptionLogMapper.insert(toEntity(event));
        } catch (Exception e) {
            log.warn("异常报文持久化失败。topic={}, stage={}", event.getTopic(), event.getRejectStage(), e);
        }
    }

    /**
     * 分页查询异常报文（按接收时间倒序）。
     */
    public PageResult query(int page, int pageSize,
                            String clientId, String topic, String rejectReason,
                            Date startTime, Date endTime) {
        PageHelper.startPage(page, pageSize);
        List<ExceptionMessageLog> list = exceptionLogMapper.selectByCondition(
                nullIfBlank(clientId), nullIfBlank(topic), nullIfBlank(rejectReason),
                startTime, endTime);
        PageInfo<ExceptionMessageLog> info = new PageInfo<>(list);
        return new PageResult(page, pageSize, (int) info.getTotal(), info.getList());
    }

    /**
     * 不分页查询全量（用于导出）。
     */
    public List<ExceptionMessageLog> selectAll(String clientId, String topic, String rejectReason,
                                               Date startTime, Date endTime) {
        return exceptionLogMapper.selectByCondition(
                nullIfBlank(clientId), nullIfBlank(topic), nullIfBlank(rejectReason),
                startTime, endTime);
    }

    private ExceptionMessageLog toEntity(MqttMessageRejectEvent event) {
        ExceptionMessageLog entity = new ExceptionMessageLog();
        entity.setReceiveTime(new Date(event.getReceiveTime()));
        entity.setClientId(event.getClientId());
        entity.setUsername(event.getUsername());
        entity.setDeviceId(event.getDeviceId());
        entity.setTopic(event.getTopic());
        entity.setPayloadSize(event.getPayload() != null ? event.getPayload().length : 0);
        if (event.getPayload() != null && event.getPayload().length > 0) {
            String raw = new String(event.getPayload(), StandardCharsets.UTF_8);
            entity.setPayload(truncate(raw, PAYLOAD_TRUNCATE_CHARS));
        }
        entity.setRejectStage(event.getRejectStage());
        entity.setRejectReason(truncate(event.getRejectReason(), REASON_TRUNCATE_CHARS));
        entity.setErrorStack(truncate(event.getErrorStack(), STACK_TRUNCATE_CHARS));
        return entity;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    public static class PageResult {
        public int pageNumber;
        public int pageSize;
        public int totalRow;
        public List<ExceptionMessageLog> list;

        public PageResult(int pageNumber, int pageSize, int totalRow, List<ExceptionMessageLog> list) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalRow = totalRow;
            this.list = list != null ? list : Collections.emptyList();
        }
    }
}
