package com.zwei.module.iot.thing.domain;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * TSL 模型服务定义
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class TslService implements Serializable {
    private static final long serialVersionUID = 658839295021228762L;

    /**
     * 服务唯一标识符（物模型模块下唯一，其中set/get是根据属性的accessMode默认生成的服务)
     */
    @ApiModelProperty("事件唯一标识符")
    private String identifier;

    /**
     * 服务名称
     */
    @ApiModelProperty("服务名称")
    private String name;

    /**
     * 服务描述
     */
    @ApiModelProperty("服务描述")
    private String desc;

    /**
     * 是否是标准功能的必选服务：是（true），否（false）
     */
    @ApiModelProperty("必选服务")
    private Boolean required;

    /**
     * 调用方式，async（异步调用）或sync（同步调用）
     */
    @ApiModelProperty("调用方式")
    private String callType;

    /**
     * 入参列表
     */
    @ApiModelProperty("入参列表")
    private List<TslParameter> inputData;

    /**
     * 出参列表
     */
    @ApiModelProperty("出参列表")
    private List<TslParameter> outputData;

    /**
     * 服务对应的方法名称（根据identifier生成）
     */
    @ApiModelProperty("方法名称")
    private String method;

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

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

    public List<TslParameter> getInputData() {
        return inputData;
    }

    public void setInputData(List<TslParameter> inputData) {
        this.inputData = inputData;
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

    @Override
    public String toString() {
        return "TslService{" +
                "callType=" + callType +
                ", identifier='" + identifier + '\'' +
                ", name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", required=" + required +
                ", inputData=" + inputData +
                ", outputData=" + outputData +
                ", method='" + method + '\'' +
                '}';
    }
}
