package com.zwei.log.infrastructure.config;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LogModulePropertiesTest {

    @Test
    void shouldSupportConfiguredLevels() {
        LogModuleProperties properties = new LogModuleProperties();
        properties.setRuntimeLevels(Set.of("error", "warn"));

        Assertions.assertTrue(properties.supportsRuntimeLevel("WARN"));
        Assertions.assertTrue(properties.supportsRuntimeLevel("ERROR"));
        Assertions.assertFalse(properties.supportsRuntimeLevel("INFO"));
    }

    @Test
    void shouldKeepBackwardCompatibilityForInfoSwitch() {
        LogModuleProperties properties = new LogModuleProperties();
        properties.setRuntimeLevels(Set.of("WARN", "ERROR"));
        properties.setRuntimeInfoEnabled(true);

        Assertions.assertTrue(properties.supportsRuntimeLevel("INFO"));
    }

    @Test
    void shouldNormalizeCheckpointFlushInterval() {
        LogModuleProperties properties = new LogModuleProperties();
        properties.setSseCheckpointFlushIntervalMs(-1L);

        Assertions.assertEquals(0L, properties.getSseCheckpointFlushIntervalMs());
    }

    @Test
    void shouldNormalizeCleanupSettings() {
        LogModuleProperties properties = new LogModuleProperties();
        properties.setCleanupRetentionDays(0);
        properties.setCleanupCheckpointRetentionDays(-3);
        properties.setCleanupBatchSize(10);

        Assertions.assertEquals(1, properties.getCleanupRetentionDays());
        Assertions.assertEquals(1, properties.getCleanupCheckpointRetentionDays());
        Assertions.assertEquals(100, properties.getCleanupBatchSize());
    }
}
