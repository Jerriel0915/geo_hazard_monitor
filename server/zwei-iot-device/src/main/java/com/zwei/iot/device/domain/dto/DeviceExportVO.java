package com.zwei.iot.device.domain.dto;

import com.zwei.common.annotation.Excel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 设备导出对象
 */
@Setter
@Getter
public class DeviceExportVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "设备编号")
    private String code;

    @Excel(name = "设备名称")
    private String name;

    @Excel(name = "设备SN")
    private String sn;

    @Excel(name = "设备类型")
    private String deviceTypeName;

    @Excel(name = "网络类型")
    private String networkTypeName;

    @Excel(name = "接入协议")
    private String protocolType;

    @Excel(name = "厂商名称")
    private String vendorName;

    @Excel(name = "经度", scale = 6)
    private Double longitude;

    @Excel(name = "纬度", scale = 6)
    private Double latitude;

    @Excel(name = "业务状态")
    private String statusName;

    @Excel(name = "在线状态")
    private String onlineStatusName;

    @Excel(name = "传感器数量")
    private Integer sensorCount;

    @Excel(name = "最近上报时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private String lastReportTime;

    @Excel(name = "创建人")
    private String createBy;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
