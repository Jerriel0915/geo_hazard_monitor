package com.zwei.module.iot.device.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 状态日志对象 zw_iot_device_alive_log
 * 
 * @author linx
 * @date 2025-09-05
 */
@ApiModel("设备状态日志对象")
public class DeviceAliveLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @ApiModelProperty("主键ID")
    private Long id;

    /** 关联设备ID */
    @Excel(name = "关联设备ID")
    @ApiModelProperty("关联设备ID")
    private Long deviceId;

    /** 上线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "上线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("上线时间")
    private Date lastConnectTime;

    /** 下线时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "下线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("下线时间")
    private Date lastDisconnectTime;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }
    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public Long getDeviceId()
    {
        return deviceId;
    }
    public void setLastConnectTime(Date lastConnectTime)
    {
        this.lastConnectTime = lastConnectTime;
    }

    public Date getLastConnectTime()
    {
        return lastConnectTime;
    }
    public void setLastDisconnectTime(Date lastDisconnectTime)
    {
        this.lastDisconnectTime = lastDisconnectTime;
    }

    public Date getLastDisconnectTime()
    {
        return lastDisconnectTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("deviceId", getDeviceId())
            .append("lastConnectTime", getLastConnectTime())
            .append("lastDisconnectTime", getLastDisconnectTime())
            .toString();
    }
}