package com.zwei.iot.core.thing.domain;

import com.zwei.iot.core.thing.domain.enums.TslAccessMode;
import com.zwei.iot.core.thing.domain.enums.TslDataTypeEnum;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * TSL 模型的属性定义
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class TslProperty implements Serializable {
    private static final long serialVersionUID = 2168227011516423150L;

    /**
     * 属性唯一标识符（物模型模块下唯一）
     */
    @ApiModelProperty("属性唯一标识符")
    private String identifier;

    /**
     * 属性名称
     */
    @ApiModelProperty("属性名称")
    private String name = "";

    /**
     * 属性读写权限
     */
    @ApiModelProperty("属性读写权限")
    private TslAccessMode accessMode = TslAccessMode.READ_WRITE;

    /**
     * 是否是标准功能的必选属性
     */
    @ApiModelProperty("是否为必选属性")
    private Boolean required = false;

    /**
     * 数据类型定义
     */
    @ApiModelProperty("数据类型定义")
    private TslDataType dataType;

    public TslProperty() {
    }

    /**
     * 只含标识符的构造函数，默认参数的类型为 text
     *
     * @param identifier 参数的唯一标识符
     */
    public TslProperty(String identifier) {
        this.identifier = identifier;
        this.dataType = new TslDataType();
        dataType.setType(TslDataTypeEnum.TEXT);
    }

    /**
     * 含参构造
     *
     * @param identifier 参数的唯一标识符
     * @param code       参数数据类型:<br>int<br>float<br>double<br>text<br>date<br>bool<br>enum<br>struct<br>array<br>具体用法参考aliyun tsl规范
     */
    public TslProperty(String identifier, String code) {
        this.identifier = identifier;
        this.dataType = new TslDataType();
        dataType.setType(TslDataTypeEnum.fromCode(code));
    }

    /**
     * 含参构造
     *
     * @param identifier 参数的唯一标识符
     * @param typeEnum   数据类型枚举类
     */
    public TslProperty(String identifier, TslDataTypeEnum typeEnum) {
        this.identifier = identifier;
        this.dataType = new TslDataType();
        dataType.setType(typeEnum);
    }

    public TslAccessMode getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(TslAccessMode accessMode) {
        this.accessMode = accessMode;
    }

    public TslDataType getDataType() {
        return dataType;
    }

    public void setDataType(TslDataType dataType) {
        this.dataType = dataType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    @Override
    public String toString() {
        return "TslProperty{" +
                "accessMode=" + accessMode +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", required=" + required +
                ", dataType=" + dataType +
                '}';
    }
}
