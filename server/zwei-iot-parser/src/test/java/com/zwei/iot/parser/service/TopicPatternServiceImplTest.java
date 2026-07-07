package com.zwei.iot.parser.service;

import com.zwei.iot.device.service.ITopicPatternService.TopicComponents;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicPatternServiceImplTest {

    @Mock
    private DataParseStrategyMapper strategyMapper;

    private TopicPatternServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TopicPatternServiceImpl();
        injectMapper(service, strategyMapper);
    }

    // --- reload + getActiveSourceTypes ---

    @Test
    @DisplayName("reload loads distinct sourceTypes from DB")
    void reload_loadsDistinctSourceTypes() {
        when(strategyMapper.selectDistinctSourceTypes())
                .thenReturn(List.of("sys", "gb", "sys"));

        service.reload();

        assertThat(service.getActiveSourceTypes()).containsExactlyInAnyOrder("sys", "gb");
    }

    @Test
    @DisplayName("reload with empty DB produces empty set")
    void reload_emptyDb_producesEmptySet() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of());

        service.reload();

        assertThat(service.getActiveSourceTypes()).isEmpty();
    }

    @Test
    @DisplayName("reload with null from mapper produces empty set")
    void reload_nullFromMapper_producesEmptySet() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(null);

        service.reload();

        assertThat(service.getActiveSourceTypes()).isEmpty();
    }

    // --- matches ---

    @Test
    @DisplayName("matches returns true for known sys topic")
    void matches_sysTopic_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        assertThat(service.matches("sys/v1/DEV001/S01/updata")).isTrue();
    }

    @Test
    @DisplayName("matches returns true for known gb topic")
    void matches_gbTopic_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        assertThat(service.matches("gb/v1/DEV002/S02/updata")).isTrue();
    }

    @Test
    @DisplayName("matches returns false for unknown prefix")
    void matches_unknownPrefix_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.matches("unknown/v1/DEV001/S01/updata")).isFalse();
    }

    @Test
    @DisplayName("matches returns false when no sourceTypes loaded")
    void matches_emptyRegistry_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of());
        service.reload();

        assertThat(service.matches("sys/v1/DEV001/S01/updata")).isFalse();
    }

    @Test
    @DisplayName("matches returns false for null topic")
    void matches_nullTopic_returnsFalse() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.matches(null)).isFalse();
    }

    @Test
    @DisplayName("matches returns true for custom sourceType with regex-safe chars")
    void matches_customSourceType_returnsTrue() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("hj", "sl"));
        service.reload();

        assertThat(service.matches("hj/v1/DEV001/S01/updata")).isTrue();
        assertThat(service.matches("sl/v1/DEV001/S01/updata")).isTrue();
    }

    // --- resolveTopic ---

    @Test
    @DisplayName("resolveTopic extracts all three components")
    void resolveTopic_sysTopic_extractsComponents() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys", "gb"));
        service.reload();

        TopicComponents c = service.resolveTopic("gb/v1/DEV999/S88/updata");

        assertThat(c).isNotNull();
        assertThat(c.sourceType()).isEqualTo("gb");
        assertThat(c.deviceCode()).isEqualTo("DEV999");
        assertThat(c.sensorCode()).isEqualTo("S88");
    }

    @Test
    @DisplayName("resolveTopic returns null for unknown topic")
    void resolveTopic_unknownPrefix_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic("xxx/v1/DEV001/S01/updata")).isNull();
    }

    @Test
    @DisplayName("resolveTopic returns null for malformed topic")
    void resolveTopic_malformedTopic_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic("sys/v1/DEV001")).isNull();
        assertThat(service.resolveTopic("sys/v1/DEV001/S01")).isNull();
    }

    @Test
    @DisplayName("resolveTopic returns null for null topic")
    void resolveTopic_nullTopic_returnsNull() {
        when(strategyMapper.selectDistinctSourceTypes()).thenReturn(List.of("sys"));
        service.reload();

        assertThat(service.resolveTopic(null)).isNull();
    }

    // --- helper ---

    private static void injectMapper(TopicPatternServiceImpl svc, DataParseStrategyMapper mapper) {
        try {
            var field = TopicPatternServiceImpl.class.getDeclaredField("strategyMapper");
            field.setAccessible(true);
            field.set(svc, mapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
