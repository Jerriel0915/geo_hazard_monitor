package com.zwei.iot.alarm.dispatch.domain.enums;

/**
 * 通知规则事件类型
 */
public enum AlarmEventType {

    THRESHOLD("THRESHOLD", "阈值告警"),
    COMPREHENSIVE("COMPREHENSIVE", "综合告警"),
    OFFLINE("OFFLINE", "设备离线");

    private final String code;
    private final String label;

    AlarmEventType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static AlarmEventType fromCode(String code) {
        for (AlarmEventType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知事件类型: " + code);
    }
}
