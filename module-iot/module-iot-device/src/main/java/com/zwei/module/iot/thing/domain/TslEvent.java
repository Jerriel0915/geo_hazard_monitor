package com.zwei.module.iot.thing.domain;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * TSL 模型事件定义
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class TslEvent implements Serializable {
    private static final long serialVersionUID = 7947702601857972349L;

    /**
     * 事件唯一标识符（物模型模块下唯一，其中post是默认生成的属性上报事件）
     */
    @ApiModelProperty("事件唯一标识符")
    private String identifier;

    /**
     * 事件名称
     */
    @ApiModelProperty("事件名称")
    private String name;

    /**
     * 事件类型（info、alert、error）
     */
    @ApiModelProperty("事件类型")
    private String type;

    /**
     * 事件描述
     */
    @ApiModelProperty("事件描述")
    private String desc;

    /**
     * 是否是标准功能的必选事件：是（true），否（false）
     */
    @ApiModelProperty("必选事件")
    private Boolean required;

    /**
     * 输出数据列表
     */
    private List<TslParameter> outputData;

    /**
     * 事件对应的方法名称（根据identifier生成）
     */
    @ApiModelProperty("方法名称")
    private String method;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TslParameter> getOutputData() {
        return outputData;
    }

    public void setOutputData(List<TslParameter> outputData) {
        this.outputData = outputData;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "TslEvent{" +
                "desc='" + desc + '\'' +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", required=" + required +
                ", outputData=" + outputData +
                ", method='" + method + '\'' +
                '}';
    }
}
