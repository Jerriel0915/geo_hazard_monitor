package com.zwei.log.infrastructure.sequence;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EventIdGeneratorTest {

    @Test
    void shouldGenerateEventIdGreaterThanLegacyMigrationRange() {
        EventIdGenerator generator = new EventIdGenerator();

        long eventId = generator.nextId();

        Assertions.assertTrue(eventId > 910_000_000_000_000_000L);
    }
}
