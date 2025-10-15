package com.zwei.monitor.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 监测点位(测站点)对象 zw_biz_monitoring_point
 * 
 * @author zwei
 * @date 2025-10-15
 */
@ApiModel("监测点位(测站点)对象")
public class MonitoringPoint extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据ID（表主键，自增） */
    @ApiModelProperty("数据ID（表主键，自增）")
    private Long id;

    /** 所属对象 */
    @Excel(name = "所属对象")
    @ApiModelProperty("所属对象")
    private Long objectId;

    /** 编号 */
    @Excel(name = "编号")
    @ApiModelProperty("编号")
    private String no;

    /** 名称 */
    @Excel(name = "名称")
    @ApiModelProperty("名称")
    private String name;

    /** 经度 */
    @Excel(name = "经度")
    @ApiModelProperty("经度")
    private Float lng;

    /** 纬度 */
    @Excel(name = "纬度")
    @ApiModelProperty("纬度")
    private Float lat;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setObjectId(Long objectId)
    {
        this.objectId = objectId;
    }

    public Long getObjectId()
    {
        return objectId;
    }
    public void setNo(String no)
    {
        this.no = no;
    }

    public String getNo()
    {
        return no;
    }
    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
    public void setLng(Float lng)
    {
        this.lng = lng;
    }

    public Float getLng()
    {
        return lng;
    }
    public void setLat(Float lat)
    {
        this.lat = lat;
    }

    public Float getLat()
    {
        return lat;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("objectId", getObjectId())
            .append("no", getNo())
            .append("name", getName())
            .append("lng", getLng())
            .append("lat", getLat())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}