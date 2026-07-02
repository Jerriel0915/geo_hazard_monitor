package com.zwei.iot.timeseries.service;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.exception.MessageRejectException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.MonitorMetadataService;
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Autowired
    public MonitorIngestFacade(MonitorTopicParser topicParser,
                               MonitorMetadataService metadataService,
                               GroovyScriptEngine scriptEngine,
                               MonitorIngestStreamService streamService) {
        this.topicParser = topicParser;
        this.metadataService = metadataService;
        this.scriptEngine = scriptEngine;
        this.streamService = streamService;
    }

    /**
     * Receive and standardize an MQTT monitoring message.
     *
     * <p>New flow: topic parse → strategy match → Groovy execute → TSL validation → Stream enqueue.
     *
     * @param topic    MQTT topic
     * @param message  raw message bytes
     * @param deviceId authenticated device primary key
     * @throws MessageRejectException when topic is malformed, no strategy matches, or parse fails
     */
    public void ingest(String topic, byte[] message, Long deviceId) {
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
                    "No matching parse strategy: sourceType=" + parsedTopic.sourceType() + ", deviceId=" + deviceId);
        }

        // 3. Execute Groovy script
        ParsedMessage parsedMessage = scriptEngine.execute(strategy, topic, message);
        if (parsedMessage == null) {
            throw new MessageRejectException("PARSE",
                    "Strategy [" + strategy.getName() + "] parse failed");
        }

        // 4. Enrich + Validate: use TSL properties (sorted by sort_order) for both
        try {
            var tsl = metadataService.getTsl(deviceId);
            if (tsl != null && tsl.properties() != null) {
                parsedMessage = enrichProperties(parsedMessage, tsl);
            }
        } catch (Exception e) {
            log.warn("TSL lookup failed, skip enrichment and validation: deviceId={}", deviceId, e);
        }

        // 5. Enqueue to Redis Stream
        streamService.enqueue(parsedMessage);
        log.debug("Monitor message enqueued, topic={}, properties={}", topic, parsedMessage.properties().size());
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
