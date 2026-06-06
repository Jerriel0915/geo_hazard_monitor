package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * 设备表 device
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Device extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备编号
     */
    private String code;

    /**
     * 设备名称
     */
    private String name;

    /**
     * 设备SN
     */
    private String sn;

    /**
     * 设备类型:0单参数,1多参数,2本地组网
     */
    private Integer deviceType;

    /**
     * 网络类型:0蜂窝,1NB-Iot
     */
    private Integer networkType;

    /**
     * 接入协议:MQTT/HTTP/COAP
     */
    private String protocolType;

    /**
     * 注册来源:MANUAL/API/IMPORT
     */
    private String registerSource;

    /**
     * 厂商名称
     */
    private String vendorName;

    /**
     * 设备接入用户名
     */
    private String authUsername;

    /**
     * 设备接入密码
     */
    private String authPassword;

    /**
     * 账号状态:1有效,2禁用
     */
    private Integer authStatus;

    /**
     * 设备图标
     */
    private String icon;

    /**
     * 图标路径
     */
    private String iconPath;

    /**
     * 业务状态（人工维护）: 1-正常, 2-故障, 3-停用
     * 实时在线状态见 device_online_status 表
     */
    private Integer status;

    /**
     * 状态名称（查询时返回）
     */
    private String statusName;

    /**
     * 运行状态: 0-未知, 1-运行中, 2-停止
     * @deprecated 实时在线状态请使用 device_online_status 表
     */
    @Deprecated
    private Integer runStatus;

    /**
     * 运行状态名称（查询时返回）
     * @deprecated 随 runStatus 废弃
     */
    @Deprecated
    private String runStatusName;

    /**
     * 最近上报时间
     */
    private String lastReportTime;

    /**
     * 注册时间
     */
    private String registeredAt;

    /**
     * 最近鉴权时间
     */
    private String lastAuthTime;

    /**
     * 最近鉴权IP
     */
    private String lastAuthIp;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 传感器列表（查询详情时返回）
     */
    private Object sensors;

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", sn='" + sn + '\'' +
                ", deviceType=" + deviceType +
                ", networkType=" + networkType +
                ", protocolType='" + protocolType + '\'' +
                ", registerSource='" + registerSource + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", authUsername='" + authUsername + '\'' +
                ", authStatus=" + authStatus +
                ", icon='" + icon + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", status=" + status +
                ", statusName='" + statusName + '\'' +
                ", runStatus=" + runStatus +
                ", runStatusName='" + runStatusName + '\'' +
                ", lastReportTime='" + lastReportTime + '\'' +
                ", registeredAt='" + registeredAt + '\'' +
                ", lastAuthTime='" + lastAuthTime + '\'' +
                ", lastAuthIp='" + lastAuthIp + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}
