package com.zwei.module.iot.product.domain;

import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import com.zwei.iot.core.thing.domain.ThingModel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 产品物模型定义对象 zw_iot_product_tsl
 *
 * @author linx
 * @date 2025-09-05
 */
@ApiModel("产品物模型定义对象")
public class ProductTsl extends BaseEntity {
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
    private ThingModel tsl;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public ThingModel getTsl() {
        return tsl;
    }

    public void setTsl(ThingModel tsl) {
        this.tsl = tsl;
    }

    @Override
    public String toString() {
        return "ProductTsl{" +
                "productId=" + productId +
                ", tsl=" + tsl +
                '}';
    }
}