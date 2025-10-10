package com.zwei.module.iot.device.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 设备基本信息对象 zw_iot_device
 * 
 * @author linx
 * @date 2025-09-05
 */
@ApiModel("设备基本信息对象")
public class Device extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    @ApiModelProperty("数据ID（表主键，自增）")
    private Long id;

    /** 设备编号;clientId */
    @Excel(name = "设备编号")
    @ApiModelProperty("设备编号;clientId")
    private String sn;

    /** 设备名称 */
    @Excel(name = "设备名称")
    @ApiModelProperty("设备名称")
    private String name;

    /** 设备类型;直连设备/网关设备/网关子设备 */
    @Excel(name = "设备类型")
    @ApiModelProperty("设备类型;直连设备/网关设备/网关子设备")
    private String type;

    /** 所属产品id */
    @Excel(name = "所属产品id")
    @ApiModelProperty("所属产品id")
    private Long productId;

    /** 通信协议 */
    @Excel(name = "通信协议")
    @ApiModelProperty("通信协议")
    private String commProtocol;

    /** 经度 */
    @Excel(name = "经度")
    @ApiModelProperty("经度")
    private Double longitude;

    /** 纬度 */
    @Excel(name = "纬度")
    @ApiModelProperty("纬度")
    private Double latitude;

    /** 供电方式 */
    @Excel(name = "供电方式")
    @ApiModelProperty("供电方式")
    private String powerSupply;

    /** 生产厂商 */
    @Excel(name = "生产厂商")
    @ApiModelProperty("生产厂商")
    private String manufacturer;

    /** 父设备id */
    @Excel(name = "父设备id")
    @ApiModelProperty("父设备id")
    private Long parentId;
    
    /** deviceKey */
    @Excel(name = "设备密钥")
    @ApiModelProperty("deviceKey")
    private String deviceKey;
    
    /** deviceSecret */
    @Excel(name = "设备密钥")
    @ApiModelProperty("deviceSecret")
    private String deviceSecret;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setSn(String sn) 
    {
        this.sn = sn;
    }

    public String getSn() 
    {
        return sn;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public String getName() 
    {
        return name;
    }

    public void setType(String type) 
    {
        this.type = type;
    }

    public String getType() 
    {
        return type;
    }

    public void setProductId(Long productId) 
    {
        this.productId = productId;
    }

    public Long getProductId() 
    {
        return productId;
    }

    public void setCommProtocol(String commProtocol) 
    {
        this.commProtocol = commProtocol;
    }

    public String getCommProtocol() 
    {
        return commProtocol;
    }

    public void setLongitude(Double longitude) 
    {
        this.longitude = longitude;
    }

    public Double getLongitude() 
    {
        return longitude;
    }

    public void setLatitude(Double latitude) 
    {
        this.latitude = latitude;
    }

    public Double getLatitude() 
    {
        return latitude;
    }

    public void setPowerSupply(String powerSupply) 
    {
        this.powerSupply = powerSupply;
    }

    public String getPowerSupply() 
    {
        return powerSupply;
    }

    public void setManufacturer(String manufacturer) 
    {
        this.manufacturer = manufacturer;
    }

    public String getManufacturer() 
    {
        return manufacturer;
    }

    public void setParentId(Long parentId) 
    {
        this.parentId = parentId;
    }

    public Long getParentId() 
    {
        return parentId;
    }
    
    public void setDeviceKey(String deviceKey) 
    {
        this.deviceKey = deviceKey;
    }

    public String getDeviceKey() 
    {
        return deviceKey;
    }
    
    public void setDeviceSecret(String deviceSecret) 
    {
        this.deviceSecret = deviceSecret;
    }

    public String getDeviceSecret() 
    {
        return deviceSecret;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("sn", getSn())
            .append("name", getName())
            .append("type", getType())
            .append("productId", getProductId())
            .append("commProtocol", getCommProtocol())
            .append("longitude", getLongitude())
            .append("latitude", getLatitude())
            .append("powerSupply", getPowerSupply())
            .append("manufacturer", getManufacturer())
            .append("parentId", getParentId())
            .append("deviceKey", getDeviceKey())
            .append("deviceSecret", getDeviceSecret())
            .toString();
    }
}
