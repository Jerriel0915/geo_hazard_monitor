package com.zwei.module.iot.device.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.zwei.common.annotation.Excel;
import com.zwei.common.core.domain.BaseEntity;

import io.swagger.annotations.ApiModelProperty;

/**
 * 设备实时状态对象 zw_iot_device_status
 * 
 * @author linx
 * @date 2025-09-05
 */
public class DeviceStatus extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    @Excel(name = "设备ID")
    @ApiModelProperty("设备ID")
    private String deviceId;

    /** 状态 0离线1在线 */
    @Excel(name = "状态", readConverterExp = "0=离线,1=在线")
    @ApiModelProperty("状态 0离线1在线")
    private Integer status;

    /** 最后上报数据时间 */
    @Excel(name = "最后上报数据时间")
    @ApiModelProperty("最后上报数据时间")
    private Long lastReportTime;

    /** 最后上线时间 */
    @Excel(name = "最后上线时间")
    @ApiModelProperty("最后上线时间")
    private Long lastConnectTime;

    /** 最后主动离线时间 */
    @Excel(name = "最后主动离线时间")
    @ApiModelProperty("最后主动离线时间")
    private Long lastOfflineTime;

    public void setDeviceId(String deviceId)
    {
        this.deviceId = deviceId;
    }

    public String getDeviceId()
    {
        return deviceId;
    }
    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getStatus()
    {
        return status;
    }
    public void setLastReportTime(Long lastReportTime)
    {
        this.lastReportTime = lastReportTime;
    }

    public Long getLastReportTime()
    {
        return lastReportTime;
    }
    public void setLastConnectTime(Long lastConnectTime)
    {
        this.lastConnectTime = lastConnectTime;
    }

    public Long getLastConnectTime()
    {
        return lastConnectTime;
    }
    public void setLastOfflineTime(Long lastOfflineTime)
    {
        this.lastOfflineTime = lastOfflineTime;
    }

    public Long getLastOfflineTime()
    {
        return lastOfflineTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("status", getStatus())
            .append("lastReportTime", getLastReportTime())
            .append("lastConnectTime", getLastConnectTime())
            .append("lastOfflineTime", getLastOfflineTime())
            .toString();
    }
}