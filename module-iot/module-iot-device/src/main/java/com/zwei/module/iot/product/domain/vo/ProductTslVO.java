package com.zwei.module.iot.product.domain.vo;

import com.alibaba.fastjson2.JSON;
import com.zwei.common.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 给前端返回的产品物模型对象 VO
 *
 * @Author: Jerriel
 * @CreateTime: 2026-01-22
 */
@ApiModel("给前端返回的产品物模型对象")
public class ProductTslVO {
    private static final long serialVersionUID = 1L;

    /**
     * 所属产品ID
     */
    @Excel(name = "所属产品ID")
    @ApiModelProperty("所属产品ID")
    private Long productId;

    /**
     * 物模型定义;TSL JSON
     */
    @Excel(name = "物模型定义")
    @ApiModelProperty("物模型定义;TSL JSON")
    private String tsl;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Object getTsl() {
        if (this.tsl == null || this.tsl.isEmpty()) {
            return null;
        }
        try {
            return JSON.parse(this.tsl);   // 返回 JSONObject/JSONArray
        } catch (Exception ignore) {
            return JSON.parse("{}");
        }
    }

    public void setTsl(String tsl) {
        this.tsl = tsl;
    }

    @Override
    public String toString() {
        return "ProductTslVO{" +
                "productId=" + productId +
                ", tsl='" + tsl + '\'' +
                '}';
    }
}
