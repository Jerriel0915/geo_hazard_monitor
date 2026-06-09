package com.zwei.iot.alarm.domain;

import java.util.Map;

/**
 * 告警模块常量。
 *
 * @author zwei
 */
public final class AlarmConstants {

    private AlarmConstants() {
    }

    /**
     * 系统操作者标识
     */
    public static final String SYSTEM_OPERATOR = "SYSTEM";

    /**
     * 告警等级 → 文本映射
     */
    public static final Map<Integer, String> LEVEL_TEXT = Map.of(
            1, "蓝色",
            2, "黄色",
            3, "橙色",
            4, "红色"
    );

    /**
     * 告警状态 → 文本映射
     */
    public static final Map<Integer, String> STATUS_TEXT = Map.of(
            1, "待处理",
            2, "处理中",
            3, "已销警",
            4, "误报"
    );

    /**
     * 根据等级值获取文本。
     */
    public static String resolveLevelText(int level) {
        return LEVEL_TEXT.getOrDefault(level, "未知");
    }

    /**
     * 根据状态值获取文本。
     */
    public static String resolveStatusName(Integer status) {
        if (status == null) return "待处理";
        return STATUS_TEXT.getOrDefault(status, "待处理");
    }
}
