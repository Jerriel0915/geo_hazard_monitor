package com.zwei.framework.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.zwei.common.config.LogSseConfig;
import com.zwei.common.utils.StringUtils;
import com.zwei.system.domain.SysOperLog;

/**
 * 操作日志事件监听器
 * 维护SSE连接池，广播日志事件，支持可配置节流
 *
 * @author zwei
 */
@Component
public class OperLogEventListener {

    private static final Logger log = LoggerFactory.getLogger(OperLogEventListener.class);

    /** SSE连接池 */
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** 最后推送时间，用于节流控制 */
    private final AtomicLong lastPushTime = new AtomicLong(0);

    /** 累计推送数量，用于节流控制 */
    private final AtomicLong pushCount = new AtomicLong(0);

    private final LogSseConfig logSseConfig;

    public OperLogEventListener(LogSseConfig logSseConfig) {
        this.logSseConfig = logSseConfig;
    }

    /**
     * 注册SSE连接
     */
    public SseEmitter register() {
        if (!logSseConfig.isEnabled()) {
            log.debug("[SSE] 日志推送未启用，拒绝注册连接");
            return null;
        }
        SseEmitter emitter = new SseEmitter((long) logSseConfig.getTimeout() * 1000);
        emitters.add(emitter);
        log.info("[SSE] 新建连接，当前活跃连接数: {}", emitters.size());

        emitter.onCompletion(() -> {
            removeEmitter(emitter);
            log.info("[SSE] 连接完成，当前活跃连接数: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            removeEmitter(emitter);
            log.info("[SSE] 连接超时，当前活跃连接数: {}", emitters.size());
        });
        emitter.onError(e -> {
            removeEmitter(emitter);
            log.warn("[SSE] 连接异常: {}，当前活跃连接数: {}", e.getMessage(), emitters.size());
        });

        return emitter;
    }

    /**
     * 监听日志事件
     */
    @EventListener
    public void onOperLogEvent(OperLogEvent event) {
        if (!logSseConfig.isEnabled()) {
            return;
        }

        // 节流控制
        if (!canPush()) {
            log.debug("[SSE] 推送触发限流，跳过本次推送");
            return;
        }

        SysOperLog operLog = event.operLog();
        String jsonData = buildJsonData(operLog);

        // 广播到所有连接
        int successCount = 0;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(jsonData);
                successCount++;
            } catch (Exception e) {
                removeEmitter(emitter);
                log.warn("[SSE] 推送失败，移除连接: {}", e.getMessage());
            }
        }
        if (successCount > 0) {
            log.info("[SSE] 推送日志成功，发送至 {} 个连接，操作: {}", successCount, operLog.getOperName());
        }
    }

    /**
     * 判断是否可以推送（节流控制）
     */
    private boolean canPush() {
        int rateLimit = logSseConfig.getRateLimit();
        if (rateLimit <= 0) {
            return true; // 不限制
        }

        long now = System.currentTimeMillis();
        long lastTime = lastPushTime.get();

        // 超过1秒，重置计数
        if (now - lastTime > 1000) {
            lastPushTime.set(now);
            pushCount.set(0);
            return true;
        }

        // 未超过1秒，检查数量
        return pushCount.incrementAndGet() <= rateLimit;
    }

    /**
     * 构建JSON数据
     */
    private String buildJsonData(SysOperLog operLog) {
        StringBuilder sb = new StringBuilder();
        sb.append("data:");
        sb.append("{\"operId\":").append(operLog.getOperId());
        sb.append(",\"title\":\"").append(nullToEmpty(operLog.getTitle())).append("\"");
        sb.append(",\"businessType\":").append(operLog.getBusinessType());
        sb.append(",\"method\":\"").append(nullToEmpty(operLog.getMethod())).append("\"");
        sb.append(",\"requestMethod\":\"").append(nullToEmpty(operLog.getRequestMethod())).append("\"");
        sb.append(",\"operName\":\"").append(nullToEmpty(operLog.getOperName())).append("\"");
        sb.append(",\"deptName\":\"").append(nullToEmpty(operLog.getDeptName())).append("\"");
        sb.append(",\"operUrl\":\"").append(nullToEmpty(operLog.getOperUrl())).append("\"");
        sb.append(",\"operIp\":\"").append(nullToEmpty(operLog.getOperIp())).append("\"");
        sb.append(",\"status\":").append(operLog.getStatus());
        sb.append(",\"costTime\":").append(operLog.getCostTime());
        sb.append(",\"operTime\":\"").append(operLog.getOperTime()).append("\"");
        sb.append("}\n\n");
        return sb.toString();
    }

    private String nullToEmpty(Object obj) {
        return obj == null ? "" : obj.toString().replace("\"", "\\\"");
    }

    /**
     * 移除断开的连接
     */
    private void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    /**
     * 获取当前连接数
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}