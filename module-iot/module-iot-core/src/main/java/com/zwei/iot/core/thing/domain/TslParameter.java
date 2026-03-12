package com.zwei.iot.core.thing.domain;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class TslParameter implements Serializable {
    private static final long serialVersionUID = -6682628119394463880L;

    /**
     * 参数唯一标识符
     */
    @ApiModelProperty("参数唯一标识符")
    private String identifier;

    /**
     * 参数名称
     */
    @ApiModelProperty("参数名称")
    private String name;

    /**
     * 数据类型定义
     */
    @ApiModelProperty("数据类型定义")
    private TslDataType dataType;

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

    @Override
    public String toString() {
        return "TslParameter{" +
                "dataType=" + dataType +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
