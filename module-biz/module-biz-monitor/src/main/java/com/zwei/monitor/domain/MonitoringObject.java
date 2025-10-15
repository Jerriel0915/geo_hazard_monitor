package com.zwei.monitor.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 监测对象基本信息表对象 zw_biz_monitoring_object
 * 
 * @author zwei
 * @date 2025-10-15
 */
@ApiModel("监测对象基本信息表对象")
public class MonitoringObject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    @ApiModelProperty("数据ID（表主键，自增）")
    private Long id;

    /** 监测对象名称 */
    @Excel(name = "监测对象名称")
    @ApiModelProperty("监测对象名称")
    private String name;

    /** 监测对象编号 */
    @Excel(name = "监测对象编号")
    @ApiModelProperty("监测对象编号")
    private String code;

    /** 监测对象类型;边坡，桥梁，河流，堤坝等 */
    @Excel(name = "监测对象类型")
    @ApiModelProperty("监测对象类型;边坡，桥梁，河流，堤坝等")
    private String type;

    /** 中心位置经度 */
    @Excel(name = "中心位置经度")
    @ApiModelProperty("中心位置经度")
    private Float centerLng;

    /** 中心位置纬度 */
    @Excel(name = "中心位置纬度")
    @ApiModelProperty("中心位置纬度")
    private Float centerLat;

    /** 对象范围;经纬坐标围成的多边形 */
    @Excel(name = "对象范围")
    @ApiModelProperty("对象范围;经纬坐标围成的多边形")
    private String monitorPolygon;

    /** 行政区划;存储一个行政区划的编码 */
    @Excel(name = "行政区划")
    @ApiModelProperty("行政区划;存储一个行政区划的编码，例如510183001002")
    private String administraRegion;

    /** 详细地址 */
    @Excel(name = "详细地址")
    @ApiModelProperty("详细地址")
    private String address;

    /** 概况 */
    @Excel(name = "概况")
    @ApiModelProperty("概况")
    private String description;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
    public void setCode(String code)
    {
        this.code = code;
    }

    public String getCode()
    {
        return code;
    }
    public void setType(String type)
    {
        this.type = type;
    }

    public String getType()
    {
        return type;
    }
    public void setCenterLng(Float centerLng)
    {
        this.centerLng = centerLng;
    }

    public Float getCenterLng()
    {
        return centerLng;
    }
    public void setCenterLat(Float centerLat)
    {
        this.centerLat = centerLat;
    }

    public Float getCenterLat()
    {
        return centerLat;
    }
    public void setMonitorPolygon(String monitorPolygon)
    {
        this.monitorPolygon = monitorPolygon;
    }

    public String getMonitorPolygon()
    {
        return monitorPolygon;
    }
    public void setAdministraRegion(String administraRegion)
    {
        this.administraRegion = administraRegion;
    }

    public String getAdministraRegion()
    {
        return administraRegion;
    }
    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getAddress()
    {
        return address;
    }
    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("name", getName())
            .append("code", getCode())
            .append("type", getType())
            .append("centerLng", getCenterLng())
            .append("centerLat", getCenterLat())
            .append("monitorPolygon", getMonitorPolygon())
            .append("administraRegion", getAdministraRegion())
            .append("address", getAddress())
            .append("description", getDescription())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}