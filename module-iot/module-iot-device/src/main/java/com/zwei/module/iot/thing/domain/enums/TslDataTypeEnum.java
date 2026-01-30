package com.zwei.module.iot.thing.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * TSL 数据类型枚举
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-28
 */
public enum TslDataTypeEnum {
    INT("int", "整数"),
    FLOAT("float", "单精度浮点"),
    DOUBLE("double", "双精度浮点"),
    TEXT("text", "字符串"),
    DATE("date", "时间"),
    BOOL("bool", "布尔型"),
    ENUM("enum", "枚举"),
    STRUCT("struct", "结构体"),
    ARRAY("array", "数组");

    private final String code;
    private final String desc;

    TslDataTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    @JsonCreator
    public static TslDataTypeEnum fromCode(String code) {
        for (TslDataTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data type: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
