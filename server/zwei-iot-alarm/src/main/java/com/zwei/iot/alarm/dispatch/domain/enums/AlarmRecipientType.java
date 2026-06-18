package com.zwei.iot.alarm.dispatch.domain.enums;

/**
 * 通知规则接收人类型
 */
public enum AlarmRecipientType {

    ROLE("ROLE", "按角色"),
    DEPT("DEPT", "按部门"),
    USER("USER", "指定人员");

    private final String code;
    private final String label;

    AlarmRecipientType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }

    public static AlarmRecipientType fromCode(String code) {
        for (AlarmRecipientType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        throw new IllegalArgumentException("未知接收人类型: " + code);
    }
}
