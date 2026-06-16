package com.zwei.iot.video.domain.dto;

import com.zwei.common.annotation.Excel;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频设备导出对象
 */
@Setter
@Getter
public class VideoDeviceExportVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Excel(name = "设备编号")
    private String code;

    @Excel(name = "设备名称")
    private String name;

    @Excel(name = "协议编码")
    private String protocolCode;

    @Excel(name = "协议名称")
    private String protocolName;

    @Excel(name = "流地址")
    private String streamUrl;

    @Excel(name = "经度", scale = 6)
    private Double longitude;

    @Excel(name = "纬度", scale = 6)
    private Double latitude;

    @Excel(name = "状态")
    private String statusName;

    @Excel(name = "最近在线时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private String lastOnlineTime;

    @Excel(name = "安装时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private String installTime;

    @Excel(name = "创建人")
    private String createBy;

    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Excel(name = "更新人")
    private String updateBy;

    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
