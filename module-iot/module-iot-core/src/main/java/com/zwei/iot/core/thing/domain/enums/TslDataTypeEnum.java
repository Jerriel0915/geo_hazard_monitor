package com.zwei.iot.core.thing.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    private static final Map<String, TslDataTypeEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(TslDataTypeEnum::getCode, Function.identity()));

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

    /**
     * 传入类型名字符串，转化为枚举类型
     *
     * @param code 类型名字符串，有int, float, double, text, date, bool, enum, struct, array
     * @return
     */
    @JsonCreator
    public static TslDataTypeEnum fromCode(String code) {
        TslDataTypeEnum type = CODE_MAP.get(code);
        // 无法找到对应数据类型时抛出 IllegalArgumentException，也可以默认设置类型为 text
        if (type == null) {
            throw new IllegalArgumentException("Unknown data type: " + code);
        }
        return type;
    }

    @Override
    public String toString() {
        return code;
    }
}
