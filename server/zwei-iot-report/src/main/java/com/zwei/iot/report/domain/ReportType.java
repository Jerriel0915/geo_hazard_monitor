package com.zwei.iot.report.domain;

/**
 * 报告类型枚举 (与 report_record.type 取值一致)。
 */
public enum ReportType {

    WEEKLY(2, "周报"),
    MONTHLY(3, "月报"),
    QUARTERLY(4, "季报");

    private final int code;
    private final String desc;

    ReportType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int code() { return code; }
    public String desc() { return desc; }

    public static ReportType fromCode(int code) {
        for (ReportType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown ReportType code: " + code);
    }
}
