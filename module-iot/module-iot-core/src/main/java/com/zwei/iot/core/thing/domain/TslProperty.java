package com.zwei.iot.core.thing.domain;

import com.zwei.iot.core.thing.domain.enums.TslAccessMode;
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
    private String name;

    /**
     * 属性读写权限
     */
    @ApiModelProperty("属性读写权限")
    private TslAccessMode accessMode;

    /**
     * 是否是标准功能的必选属性
     */
    @ApiModelProperty("是否为必选属性")
    private Boolean required;

    /**
     * 数据类型定义
     */
    @ApiModelProperty("数据类型定义")
    private TslDataType dataType;

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
