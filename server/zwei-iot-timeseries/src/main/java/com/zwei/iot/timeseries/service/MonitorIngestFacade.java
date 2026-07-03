package com.zwei.iot.timeseries.service;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.event.MqttMessageRejectEvent;
import com.zwei.common.exception.MessageRejectException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.MonitorMetadataService;
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * MQTT monitoring data ingest facade — rewritten to use parser module.
 *
 * <p>Replaces the old MonitorPayloadParser-based flow with strategy matching
 * and Groovy script execution, producing TSL-aligned ParsedMessage via Redis Stream.
 */
@Slf4j
@Service
public class MonitorIngestFacade {

    private static final Pattern POSITIONAL_PATTERN = Pattern.compile("value_\\d+");
    private final MonitorTopicParser topicParser;
    private final MonitorMetadataService metadataService;
    private final GroovyScriptEngine scriptEngine;
    private final MonitorIngestStreamService streamService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 独立线程池处理 Groovy 解析——将耗时的脚本执行移出 MQTT IO 线程，
     * 避免单个慢脚本阻塞所有设备的 MQTT 消息处理。
     */
    private final ExecutorService ingestExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "monitor-ingest-worker");
                t.setDaemon(true);
                return t;
            });

    @Autowired
    public MonitorIngestFacade(MonitorTopicParser topicParser,
                               MonitorMetadataService metadataService,
                               GroovyScriptEngine scriptEngine,
                               MonitorIngestStreamService streamService,
                               ApplicationEventPublisher eventPublisher) {
        this.topicParser = topicParser;
        this.metadataService = metadataService;
        this.scriptEngine = scriptEngine;
        this.streamService = streamService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 异步入口——MQTT IO 线程调用后立即返回。
     *
     * <p>topic 解析 → 策略匹配 → Groovy 脚本执行 → TSL 校验 → Stream 入队
     * 全部在 ingestExecutor 线程池中异步执行，不阻塞 MQTT IO 线程。
     *
     * @param topic    MQTT topic
     * @param message  raw message bytes
     * @param deviceId authenticated device primary key
     */
    public void ingest(String topic, byte[] message, Long deviceId) {
        ingestExecutor.submit(() -> {
            try {
                doIngest(topic, message, deviceId);
            } catch (Exception e) {
                log.error("异步 ingest 任务异常退出: topic={}, deviceId={}", topic, deviceId, e);
            }
        });
    }

    /** 同步执行体——在 ingestExecutor 线程池中运行 */
    void doIngest(String topic, byte[] message, Long deviceId) {
        try {
            // 1. Parse topic
            MonitorTopic parsedTopic = topicParser.parse(topic);
            if (parsedTopic == null) {
                throw new MessageRejectException("FORMAT", "Invalid monitor topic format: " + topic);
            }

            // 2. Match strategy
            DataParseStrategy strategy = metadataService.resolveStrategy(
                    parsedTopic.sourceType(), deviceId);
            if (strategy == null) {
                throw new MessageRejectException("STRATEGY",
                        "No matching parse strategy: sourceType=" + parsedTopic.sourceType()
                                + ", deviceId=" + deviceId);
            }

            // 3. Execute Groovy script
            ParsedMessage parsedMessage = scriptEngine.execute(strategy, topic, message);
            if (parsedMessage == null) {
                throw new MessageRejectException("PARSE",
                        "Strategy [" + strategy.getName() + "] parse failed");
            }

            // 4. Enrich + Validate: use TSL properties for both
            try {
                var tsl = metadataService.getTsl(deviceId);
                if (tsl != null && tsl.properties() != null) {
                    parsedMessage = enrichProperties(parsedMessage, tsl);
                }
            } catch (Exception e) {
                log.warn("TSL lookup failed, skip enrichment: deviceId={}", deviceId, e);
            }

            // 5. Enqueue to Redis Stream
            streamService.enqueue(parsedMessage);
            log.debug("Monitor message enqueued, topic={}, properties={}",
                    topic, parsedMessage.properties().size());
        } catch (MessageRejectException e) {
            publishReject(deviceId, topic, message, e.getRejectStage(), e.getMessage(), null);
        } catch (Exception e) {
            log.error("监测消息处理失败 topic={}, deviceId={}", topic, deviceId, e);
            publishReject(deviceId, topic, message, "UNKNOWN", e.getMessage(), getStackTrace(e));
        }
    }

    @PreDestroy
    public void shutdown() {
        ingestExecutor.shutdown();
        try {
            if (!ingestExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                ingestExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            ingestExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** 发布异常报文事件 */
    private void publishReject(Long deviceId, String topic, byte[] message,
                               String rejectStage, String rejectReason, String errorStack) {
        try {
            eventPublisher.publishEvent(new MqttMessageRejectEvent(
                    null, null, deviceId, topic, message, System.currentTimeMillis(),
                    rejectStage, rejectReason, errorStack));
        } catch (Exception ex) {
            log.warn("发布异常报文事件失败。topic={}, stage={}", topic, rejectStage, ex);
        }
    }

    /** 获取异常堆栈字符串 */
    private static String getStackTrace(Throwable t) {
        if (t == null) return null;
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Enrich: map positional value_N identifiers to real TSL attribute codes.
     * Validate: check value against TSL specs (min/max) — warn only, never block.
     *
     * <p>TSL properties are pre-sorted by {@code monitor_content.sort_order}, so positional
     * identifiers map directly to the corresponding TSL property index.</p>
     */
    private ParsedMessage enrichProperties(ParsedMessage message,
                                            com.zwei.iot.device.domain.tsl.ProductTsl tsl) {
        List<com.zwei.common.domain.PropertyValue> props = message.properties();
        // Fast path: no positional identifiers
        boolean hasPositional = false;
        for (var p : props) {
            if (p.identifier() != null && POSITIONAL_PATTERN.matcher(p.identifier()).matches()) {
                hasPositional = true;
                break;
            }
        }
        if (!hasPositional) return message;

        List<com.zwei.iot.device.domain.tsl.TslProperty> tslProps = tsl.properties();
        List<com.zwei.common.domain.PropertyValue> enriched = new java.util.ArrayList<>();
        for (var p : props) {
            String id = p.identifier();
            if (id != null && POSITIONAL_PATTERN.matcher(id).matches()) {
                int idx = Integer.parseInt(id.substring(6));
                if (idx >= 0 && idx < tslProps.size()) {
                    var tslProp = tslProps.get(idx);
                    String unit = (tslProp.dataType() != null && tslProp.dataType().specs() != null)
                            ? tslProp.dataType().specs().unit() : null;
                    enriched.add(new com.zwei.common.domain.PropertyValue(
                            tslProp.identifier(), tslProp.name(), unit, p.value(), p.quality()));
                    validateValue(tslProp, p.value());
                } else {
                    log.warn("Positional identifier {} out of range (TSL properties size={}), keeping as-is",
                            id, tslProps.size());
                    enriched.add(p);
                }
            } else {
                enriched.add(p);
            }
        }
        return new ParsedMessage(message.deviceCode(), message.sensorCode(), message.sourceType(),
                message.dataTime(), message.receiveTime(), message.payloadHash(), enriched);
    }

    private void validateValue(com.zwei.iot.device.domain.tsl.TslProperty tslProp, Object value) {
        if (tslProp.dataType() == null || tslProp.dataType().specs() == null) return;
        if (!(value instanceof Number)) return; // 非数值跳过 min/max 校验
        var specs = tslProp.dataType().specs();
        double dv = ((Number) value).doubleValue();
        if (specs.min() != null && dv < Double.parseDouble(specs.min())) {
            log.warn("Property value below min: {}={}, min={}", tslProp.identifier(), value, specs.min());
        }
        if (specs.max() != null && dv > Double.parseDouble(specs.max())) {
            log.warn("Property value exceeds max: {}={}, max={}", tslProp.identifier(), value, specs.max());
        }
    }
}
