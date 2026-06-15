package com.zwei.iot.timeseries.service;

import com.zwei.common.domain.ParsedMessage;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.MonitorMetadataService;
import com.zwei.iot.parser.support.MonitorTopic;
import com.zwei.iot.parser.support.MonitorTopicParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQTT monitoring data ingest facade — rewritten to use parser module.
 *
 * <p>Replaces the old MonitorPayloadParser-based flow with strategy matching
 * and Groovy script execution, producing TSL-aligned ParsedMessage via Redis Stream.
 */
@Slf4j
@Service
public class MonitorIngestFacade {
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
     * @throws ServiceException when topic is malformed
     */
    public void ingest(String topic, byte[] message, Long deviceId) {
        // 1. Parse topic
        MonitorTopic parsedTopic = topicParser.parse(topic);
        if (parsedTopic == null) {
            throw new ServiceException("Invalid monitor topic format: " + topic);
        }

        // 2. Match strategy
        DataParseStrategy strategy = metadataService.resolveStrategy(
                parsedTopic.sourceType(), deviceId);
        if (strategy == null) {
            log.error("No matching parse strategy found: sourceType={}, deviceId={}, topic={}",
                    parsedTopic.sourceType(), deviceId, topic);
            return; // silent discard, MQTT won't ack
        }

        // 3. Execute Groovy script
        ParsedMessage parsedMessage = scriptEngine.execute(strategy, topic, message);
        if (parsedMessage == null) {
            String payloadStr = new String(message, StandardCharsets.UTF_8);
            streamService.enqueueDeadLetter(topic, payloadStr,
                    "Strategy [" + strategy.getName() + "] parse failed");
            return;
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
            if (p.identifier() != null && p.identifier().matches("value_\\d+")) {
                hasPositional = true;
                break;
            }
        }
        if (!hasPositional) return message;

        List<com.zwei.iot.device.domain.tsl.TslProperty> tslProps = tsl.properties();
        List<com.zwei.common.domain.PropertyValue> enriched = new java.util.ArrayList<>();
        for (var p : props) {
            String id = p.identifier();
            if (id != null && id.matches("value_\\d+")) {
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

    private void validateValue(com.zwei.iot.device.domain.tsl.TslProperty tslProp, Double value) {
        if (tslProp.dataType() == null || tslProp.dataType().specs() == null) return;
        var specs = tslProp.dataType().specs();
        if (specs.min() != null && value != null
                && value < Double.parseDouble(specs.min())) {
            log.warn("Property value below min: {}={}, min={}", tslProp.identifier(), value, specs.min());
        }
        if (specs.max() != null && value != null
                && value > Double.parseDouble(specs.max())) {
            log.warn("Property value exceeds max: {}={}, max={}", tslProp.identifier(), value, specs.max());
        }
    }
}
