package com.zwei.iot.monitor.constant;

import java.util.Map;

/**
 * 监测内容 indicator_type → 数据类型 (valueType) 映射。
 *
 * <p>复用 monitor_content.indicator_type 字段作为类型判别，避免新增 DB 列。
 * 未知 code 默认 NUMBER（向后兼容）。
 *
 * <p>valueType 取值: NUMBER / DATETIME / STRING / BOOLEAN
 */
public final class IndicatorValueType {

    public static final String NUMBER = "NUMBER";
    public static final String DATETIME = "DATETIME";
    public static final String STRING = "STRING";
    public static final String BOOLEAN = "BOOLEAN";

    private static final Map<String, String> MAPPING = Map.ofEntries(
            Map.entry("wy", NUMBER),    // 位移
            Map.entry("wd", NUMBER),    // 温度
            Map.entry("jd", NUMBER),    // 角度
            Map.entry("yl", NUMBER),    // 压力
            Map.entry("sw", NUMBER),    // 水位
            Map.entry("jsd", NUMBER),   // 加速度
            Map.entry("hsl", NUMBER),   // 含水率
            Map.entry("ljn", NUMBER),   // 力矩
            Map.entry("zdl", NUMBER),   // 震动频率
            Map.entry("dl", NUMBER),    // 电量
            Map.entry("dx", BOOLEAN),   // 断线
            Map.entry("sg", STRING),    // 声光
            Map.entry("sp", STRING)     // 视频
    );

    private IndicatorValueType() {}

    /** 返回 indicator_type code 对应的 valueType；未知返回 NUMBER。 */
    public static String of(String indicatorType) {
        if (indicatorType == null || indicatorType.isBlank()) return NUMBER;
        return MAPPING.getOrDefault(indicatorType.trim().toLowerCase(), NUMBER);
    }
}
