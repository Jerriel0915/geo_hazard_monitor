package com.zwei.monitor.domain;

import com.zwei.module.iot.device.domain.Device;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 点位关联设备实体类
 * 
 * @author zwei
 * @date 2025-10-15
 */
@ApiModel("点位关联设备实体类")
public class PointDevice extends Device
{
    private static final long serialVersionUID = 1L;

    /** 测点ID */
    @ApiModelProperty("测点ID")
    private Long pointId;

    /** 测点名称 */
    @ApiModelProperty("测点名称")
    private String pointName;

    /** 测点编号 */
    @ApiModelProperty("测点编号")
    private String pointCode;

    /** 映射关系状态（0正常 1停用） */
    @ApiModelProperty("映射关系状态")
    private String status;

    /** 备注 */
    @ApiModelProperty("备注")
    private String remark;

    /**
     * 获取测点ID
     */
    public Long getPointId()
    {
        return pointId;
    }

    /**
     * 设置测点ID
     */
    public void setPointId(Long pointId)
    {
        this.pointId = pointId;
    }

    /**
     * 获取测点名称
     */
    public String getPointName()
    {
        return pointName;
    }

    /**
     * 设置测点名称
     */
    public void setPointName(String pointName)
    {
        this.pointName = pointName;
    }

    /**
     * 获取测点编号
     */
    public String getPointCode()
    {
        return pointCode;
    }

    /**
     * 设置测点编号
     */
    public void setPointCode(String pointCode)
    {
        this.pointCode = pointCode;
    }

    /**
     * 获取映射关系状态
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * 设置映射关系状态
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * 获取备注
     */
    public String getRemark()
    {
        return remark;
    }

    /**
     * 设置备注
     */
    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}