package com.zwei.iot.core.thing.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * TSL 属性读写权限枚举
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-28
 */
public enum TslAccessMode {

    @JsonProperty("r")
    READ_ONLY("r", "只读"),

    @JsonProperty("rw")
    READ_WRITE("rw", "读写");

    private final String code;
    private final String desc;

    TslAccessMode(String code, String desc) {
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
    public static TslAccessMode fromCode(String code) {
        for (TslAccessMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown access mode: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}
