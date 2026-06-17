package com.zwei.iot.report.support;

import com.zwei.iot.report.domain.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportPeriod 周期计算")
class ReportPeriodTest {

    @Nested
    @DisplayName("lastWeek")
    class LastWeek {
        @Test void normal() {
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 6, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 6, 8));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 14));
        }
        @Test void crossYear() {
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 1, 1));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 12, 22));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 28));
        }
        @Test void mondayIsStart() {
            // 2026-06-15 是周一, 上周应回退 7 天
            ReportPeriod p = ReportPeriod.lastWeek(LocalDate.of(2026, 6, 15));
            assertThat(p.start().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(p.end().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        }
    }

    @Nested
    @DisplayName("lastMonth")
    class LastMonth {
        @Test void normal() {
            // 月报定时任务在每月 1 日执行,处理上个月。模拟 7 月 1 日跑 6 月报。
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 7, 1));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 30));
        }
        @Test void crossYear() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 1, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 12, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 31));
        }
        @Test void leapYearFebruary() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2024, 3, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2024, 2, 29));
        }
        @Test void nonLeapYearFebruary() {
            ReportPeriod p = ReportPeriod.lastMonth(LocalDate.of(2026, 3, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 2, 28));
        }
    }

    @Nested
    @DisplayName("lastQuarter")
    class LastQuarter {
        @Test void currentQ1_lastIsPrevQ4() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 1, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2025, 10, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2025, 12, 31));
        }
        @Test void currentQ2_lastIsQ1() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 4, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 3, 31));
        }
        @Test void currentQ3_lastIsQ2() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 7, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 6, 30));
        }
        @Test void currentQ4_lastIsQ3() {
            ReportPeriod p = ReportPeriod.lastQuarter(LocalDate.of(2026, 10, 15));
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 9, 30));
        }
    }

    @Test
    @DisplayName("previous(WEEKLY, today) 等价于 lastWeek(today)")
    void previousDispatch() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        assertThat(ReportPeriod.previous(ReportType.WEEKLY, today))
            .isEqualTo(ReportPeriod.lastWeek(today));
        assertThat(ReportPeriod.previous(ReportType.MONTHLY, today))
            .isEqualTo(ReportPeriod.lastMonth(today));
        assertThat(ReportPeriod.previous(ReportType.QUARTERLY, today))
            .isEqualTo(ReportPeriod.lastQuarter(today));
    }

    @Test
    @DisplayName("key() 返回 start_end 标识")
    void keyFormat() {
        ReportPeriod p = new ReportPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 14));
        assertThat(p.key()).isEqualTo("2026-06-08_2026-06-14");
    }
}
