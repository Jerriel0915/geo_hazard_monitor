package com.zwei.module.iot.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 产品物模型定义对象 zw_iot_product_tsl
 * 
 * @author linx
 * @date 2025-09-05
 */
@ApiModel("产品物模型定义对象")
public class ProductTsl extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 所属产品ID */
    @Excel(name = "所属产品ID")
    @ApiModelProperty("所属产品ID")
    private Long productId;

    /** 物模型定义;TSL JSON */
    @Excel(name = "物模型定义")
    @ApiModelProperty("物模型定义;TSL JSON")
    private String tsl;

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setTsl(String tsl)
    {
        this.tsl = tsl;
    }

    public String getTsl()
    {
        return tsl;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("productId", getProductId())
            .append("tsl", getTsl())
            .toString();
    }
}