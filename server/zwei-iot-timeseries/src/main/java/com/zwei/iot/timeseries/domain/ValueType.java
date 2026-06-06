package com.zwei.iot.timeseries.domain;

/**
 * 监测数据值类型枚举。
 *
 * <p>定义时序数据查询的聚合粒度：</p>
 * <ul>
 *   <li>{@code CURRENT} — 原始数据，不做聚合</li>
 *   <li>{@code HOUR} — 按小时聚合（GROUP BY 1h）</li>
 *   <li>{@code H24} — 按天聚合（GROUP BY 1d）</li>
 *   <li>{@code H72} — 按三天聚合（GROUP BY 3d）</li>
 * </ul>
 */
public enum ValueType {

    CURRENT("current", null, null),
    HOUR("hour", "1h", "AVG"),
    H24("24h", "1d", "AVG"),
    H72("72h", "3d", "AVG");

    private final String code;
    private final String groupInterval;
    private final String aggFunction;

    ValueType(String code, String groupInterval, String aggFunction) {
        this.code = code;
        this.groupInterval = groupInterval;
        this.aggFunction = aggFunction;
    }

    public String getCode() {
        return code;
    }

    public String getGroupInterval() {
        return groupInterval;
    }

    public String getAggFunction() {
        return aggFunction;
    }

    public boolean isAggregated() {
        return groupInterval != null;
    }

    /**
     * 根据编码解析值类型。
     *
     * @param code 类型编码，可空
     * @return 对应的值类型；为空或无法识别时默认返回 CURRENT
     */
    public static ValueType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return CURRENT;
        }
        for (ValueType vt : values()) {
            if (vt.code.equalsIgnoreCase(code)) {
                return vt;
            }
        }
        return CURRENT;
    }
}
