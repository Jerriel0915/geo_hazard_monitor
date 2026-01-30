package com.zwei.module.iot.thing.domain;

import com.alibaba.fastjson2.JSONObject;
import com.zwei.module.iot.thing.domain.enums.TslDataTypeEnum;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * TSL 模型的数据类型定义
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class TslDataType implements Serializable {
    private static final long serialVersionUID = 2863583234434351284L;

    /**
     * 具体数据类型，使用枚举
     */
    @ApiModelProperty("数据类型")
    private TslDataTypeEnum type;

    /**
     * 数据类型的具体定义
     */
    @ApiModelProperty("数据类型定义")
    private JSONObject specs;

    public JSONObject getSpecs() {
        return specs;
    }

    public void setSpecs(JSONObject specs) {
        this.specs = specs;
    }

    public TslDataTypeEnum getType() {
        return type;
    }

    public void setType(TslDataTypeEnum type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TslDataType{" +
                "specs=" + specs +
                ", type=" + type +
                '}';
    }
}
