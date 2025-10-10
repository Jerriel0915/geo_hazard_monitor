package com.zwei.module.iot.product.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 产品对象 zw_iot_product
 * 
 * @author linx
 * @date 2025-09-05
 */
@ApiModel("产品对象")
public class Product extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    @ApiModelProperty("数据ID（表主键，自增）")
    private Long id;

    /** productKey;协议接入时topic使用 */
    @Excel(name = "产品密钥")
    @ApiModelProperty("productKey;协议接入时topic使用")
    private String productKey;

    /** 产品名称 */
    @Excel(name = "产品名称")
    @ApiModelProperty("产品名称")
    private String name;

    /** 设备类型;0:直连设备,1:网关,2:传感器 */
    @Excel(name = "设备类型")
    @ApiModelProperty("设备类型;0:直连设备,1:网关,2:传感器")
    private Integer nodeType;

    /** 描述 */
    @Excel(name = "描述")
    @ApiModelProperty("描述")
    private String remarks;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setProductKey(String productKey)
    {
        this.productKey = productKey;
    }

    public String getProductKey()
    {
        return productKey;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public void setNodeType(Integer nodeType)
    {
        this.nodeType = nodeType;
    }

    public Integer getNodeType()
    {
        return nodeType;
    }

    public void setRemarks(String remarks)
    {
        this.remarks = remarks;
    }

    public String getRemarks()
    {
        return remarks;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("productKey", getProductKey())
            .append("name", getName())
            .append("nodeType", getNodeType())
            .append("remarks", getRemarks())
            .toString();
    }
}
