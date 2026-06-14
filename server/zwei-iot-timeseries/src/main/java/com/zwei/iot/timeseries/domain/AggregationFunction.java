package com.zwei.iot.timeseries.domain;

/**
 * 聚合函数白名单枚举。
 *
 * <p>封闭枚举,SQL 拼接只能从这里取值,无法注入任意函数。
 * 支持普通聚合 (AVG/MAX/MIN/SUM/COUNT/FIRST_VALUE/LAST_VALUE/EXTREME/STDDEV)
 * 与百分位 (P50/P95/P99,IoTDB 用 QUANTILE 实现)。</p>
 */
public enum AggregationFunction {
    AVG("AVG", null),
    MAX("MAX", null),
    MIN("MIN", null),
    SUM("SUM", null),
    COUNT("COUNT", null),
    FIRST_VALUE("FIRST_VALUE", null),
    LAST_VALUE("LAST_VALUE", null),
    EXTREME("EXTREME", null),
    STDDEV("STDDEV", null),
    P50("QUANTILE", 0.5),
    P95("QUANTILE", 0.95),
    P99("QUANTILE", 0.99);

    private final String iotdbFunc;
    private final Double quartileParam;

    AggregationFunction(String iotdbFunc, Double quartileParam) {
        this.iotdbFunc = iotdbFunc;
        this.quartileParam = quartileParam;
    }

    /**
     * 渲染为 IoTDB 表达式字符串。
     *
     * @param attrCode 业务指标编码,必须已通过 IotdbPathResolver 校验
     * @return IoTDB 表达式,如 {@code AVG(value)} 或 {@code QUANTILE(value, 0.95)}
     */
    public String getIotdbExpr(String attrCode) {
        if (quartileParam != null) {
            return "QUANTILE(" + attrCode + ", " + quartileParam + ")";
        }
        return iotdbFunc + "(" + attrCode + ")";
    }

    public boolean needsQuartileParam() {
        return quartileParam != null;
    }
}
