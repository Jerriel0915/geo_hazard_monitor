package com.zwei.iot.report.support;

import com.zwei.iot.report.domain.ReportType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * 报告周期 (闭区间)。
 * 不可变值对象, 用 java.time 计算上周/上月/上季度。
 */
public record ReportPeriod(LocalDate start, LocalDate end) {

    /** 上一自然周 (周一~周日) */
    public static ReportPeriod lastWeek(LocalDate today) {
        LocalDate monday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        return new ReportPeriod(monday, monday.plusDays(6));
    }

    /** 上一自然月 */
    public static ReportPeriod lastMonth(LocalDate today) {
        LocalDate first = today.minusMonths(1).withDayOfMonth(1);
        return new ReportPeriod(first, first.withDayOfMonth(first.lengthOfMonth()));
    }

    /** 上一自然季度 (Q1=1-3, Q2=4-6, Q3=7-9, Q4=10-12) */
    public static ReportPeriod lastQuarter(LocalDate today) {
        LocalDate currentQuarterStart = today.with(IsoFields.DAY_OF_QUARTER, 1);
        LocalDate first = currentQuarterStart.minusMonths(3);
        LocalDate last = first.plusMonths(3).minusDays(1);
        return new ReportPeriod(first, last);
    }

    /** 按 ReportType 分发到对应方法 */
    public static ReportPeriod previous(ReportType type, LocalDate today) {
        return switch (type) {
            case WEEKLY -> lastWeek(today);
            case MONTHLY -> lastMonth(today);
            case QUARTERLY -> lastQuarter(today);
        };
    }

    /** 唯一 key, 用于 Redis 锁与日志 */
    public String key() {
        return start() + "_" + end();
    }
}
