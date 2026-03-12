package com.zwei.iot.core.thing.domain;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 物模型基本定义
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-26
 */
public class ThingModel implements Serializable {
    private static final long serialVersionUID = 6331688563289251271L;

    /**
     * 物模型结构定义的访问URL，暂时用不上，置空
     */
    @ApiModelProperty("物模型的访问URL")
    private String schema = "";

    /**
     * 物模型配置信息
     */
    @ApiModelProperty("物模型相关配置信息")
    private Profile profile;

    /**
     * 属性列表
     */
    @ApiModelProperty("属性列表")
    private List<TslProperty> properties;

    /**
     * 事件列表
     */
    @ApiModelProperty("事件列表")
    private List<TslEvent> events;

    /**
     * 服务列表
     */
    @ApiModelProperty("服务列表")
    private List<TslService> services;

    public ThingModel() {
    }

    public ThingModel(String productKey) {
        this.profile = new Profile(productKey);
        this.properties = new ArrayList<>();
        this.events = new ArrayList<>();
        this.services = new ArrayList<>();
    }

    /**
     * 物模型相关配置信息，包含 productKey
     */
    public static class Profile implements Serializable {
        private static final long serialVersionUID = 3448301744949214292L;
        /**
         * 当前产品的ProductKey
         */
        @ApiModelProperty("当前产品的ProductKey")
        private String productKey;

        public Profile() {
        }

        public Profile(String productKey) {
            this.productKey = productKey;
        }

        public String getProductKey() {
            return productKey;
        }

        public void setProductKey(String productKey) {
            this.productKey = productKey;
        }

        @Override
        public String toString() {
            return "Profile{" +
                    "productKey='" + productKey + '\'' +
                    '}';
        }
    }

    public List<TslEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TslEvent> events) {
        this.events = events;
    }

    public Profile getProfile() {
        return profile.productKey == null || profile.productKey.isEmpty() ? null : profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public List<TslProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<TslProperty> properties) {
        this.properties = properties;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<TslService> getServices() {
        return services;
    }

    public void setServices(List<TslService> services) {
        this.services = services;
    }

    @Override
    public String toString() {
        return "ThingModel{" +
                "events=" + events +
                ", schema='" + schema + '\'' +
                ", profile=" + profile +
                ", properties=" + properties +
                ", services=" + services +
                '}';
    }
}
