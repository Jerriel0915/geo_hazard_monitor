package com.zwei.iot.timeseries.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.zwei.iot.timeseries.domain.TimeWindowSpec.WindowGranularity.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeWindowSpec")
class TimeWindowSpecTest {

    @Test
    @DisplayName("RAW 粒度 GROUP BY 间隔为 null")
    void raw_noGroupBy() {
        TimeWindowSpec spec = new TimeWindowSpec(0L, 1000L, RAW);
        assertThat(spec.granularity().toGroupByInterval()).isNull();
    }

    @Test
    @DisplayName("HOUR 粒度 GROUP BY 间隔为 1h")
    void hour_groupBy() {
        assertThat(new TimeWindowSpec(0L, 1000L, HOUR).granularity().toGroupByInterval()).isEqualTo("1h");
    }

    @Test
    @DisplayName("DAY 粒度 GROUP BY 间隔为 1d")
    void day_groupBy() {
        assertThat(new TimeWindowSpec(0L, 1000L, DAY).granularity().toGroupByInterval()).isEqualTo("1d");
    }

    @Test
    @DisplayName("CUSTOM 粒度需要传入 customMillis")
    void custom_groupBy() {
        TimeWindowSpec spec = new TimeWindowSpec(0L, 1000L, CUSTOM);
        assertThat(spec.granularity().toGroupByInterval()).isEqualTo("?");
        assertThat(spec.granularity().toGroupByInterval(60000L)).isEqualTo("60000ms");
    }

    @Test
    @DisplayName("isAggregated — RAW 返回 false,其他返回 true")
    void isAggregated() {
        assertThat(RAW.isAggregated()).isFalse();
        assertThat(HOUR.isAggregated()).isTrue();
        assertThat(DAY.isAggregated()).isTrue();
        assertThat(CUSTOM.isAggregated()).isTrue();
    }

    @Test
    @DisplayName("TimeWindowSpec 持有 startTime / endTime / granularity")
    void record_holdsFields() {
        TimeWindowSpec spec = new TimeWindowSpec(100L, 200L, HOUR);
        assertThat(spec.startTime()).isEqualTo(100L);
        assertThat(spec.endTime()).isEqualTo(200L);
        assertThat(spec.granularity()).isEqualTo(HOUR);
    }
}
