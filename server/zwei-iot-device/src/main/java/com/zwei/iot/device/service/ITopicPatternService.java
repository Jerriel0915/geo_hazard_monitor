package com.zwei.iot.device.service;

import java.util.Set;

/**
 * MQTT topic pattern registry — dynamic topic prefix validation.
 *
 * <p>Derives active topic prefixes from {@code DataParseStrategy.sourceType},
 * replacing hardcoded {@code sys/v1/} and {@code gb/v1/} checks across the broker
 * and parser modules.
 *
 * <p>Path structure <code>{sourceType}/v1/{deviceCode}/{sensorCode}/updata</code>
 * is fixed; only the first segment varies.
 */
public interface ITopicPatternService {

    /**
     * Check if a topic matches any registered protocol prefix.
     *
     * @param topic MQTT topic string
     * @return true if the topic matches a known pattern
     */
    boolean matches(String topic);

    /**
     * Extract structured components from a topic.
     *
     * @param topic MQTT topic string
     * @return parsed components, or {@code null} if no pattern matches
     */
    TopicComponents resolveTopic(String topic);

    /**
     * Current active sourceType set (read-only snapshot).
     */
    Set<String> getActiveSourceTypes();

    /**
     * Force reload the pattern registry from the database.
     */
    void reload();

    /**
     * Structured representation of a parsed MQTT monitor topic.
     */
    record TopicComponents(String sourceType, String deviceCode, String sensorCode) {
    }
}
