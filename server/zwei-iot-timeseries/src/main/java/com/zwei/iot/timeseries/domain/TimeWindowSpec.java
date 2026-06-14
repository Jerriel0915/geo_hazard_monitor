package com.zwei.iot.timeseries.domain;

/**
 * 时间窗口 + 聚合粒度参数。
 *
 * <p>粒度决定是否走 IoTDB GROUP BY 降采样。</p>
 *
 * @param startTime   开始时间(毫秒时间戳),可空
 * @param endTime     结束时间(毫秒时间戳),可空
 * @param granularity 粒度
 */
public record TimeWindowSpec(Long startTime, Long endTime, WindowGranularity granularity) {

    /**
     * 聚合粒度,决定 IoTDB GROUP BY 间隔。
     */
    public enum WindowGranularity {
        RAW(null),
        HOUR("1h"),
        DAY("1d"),
        CUSTOM("?");

        private final String defaultInterval;

        WindowGranularity(String defaultInterval) {
            this.defaultInterval = defaultInterval;
        }

        public String toGroupByInterval() {
            return defaultInterval;
        }

        public String toGroupByInterval(Long customMillis) {
            if (this != CUSTOM) {
                return defaultInterval;
            }
            if (customMillis == null || customMillis <= 0) {
                throw new IllegalArgumentException("CUSTOM 粒度必须传入正整数毫秒值");
            }
            return customMillis + "ms";
        }

        public boolean isAggregated() {
            return this != RAW;
        }
    }
}
