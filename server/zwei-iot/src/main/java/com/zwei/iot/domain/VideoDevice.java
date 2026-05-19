package com.zwei.iot.domain;

import com.zwei.common.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 视频设备表 video_device
 *
 * @author zwei
 */
public class VideoDevice extends BaseEntity {
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
     * 图标代码
     */
    private String icon;

    /**
     * 图标路径
     */
    private String iconPath;

    /**
     * 协议类型编码
     */
    private String protocolCode;

    /**
     * 协议类型名称
     */
    private String protocolName;

    /**
     * 视频流地址
     */
    private String streamUrl;

    /**
     * 状态: 0-离线, 1-在线, 2-故障
     */
    private Integer status;

    /**
     * 最近在线时间
     */
    private String lastOnlineTime;

    /**
     * 安装时间
     */
    private String installTime;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    public String getProtocolCode() {
        return protocolCode;
    }

    public void setProtocolCode(String protocolCode) {
        this.protocolCode = protocolCode;
    }

    public String getProtocolName() {
        return protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.protocolName = protocolName;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(String lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public String getInstallTime() {
        return installTime;
    }

    public void setInstallTime(String installTime) {
        this.installTime = installTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return "VideoDevice{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", protocolCode='" + protocolCode + '\'' +
                ", protocolName='" + protocolName + '\'' +
                ", streamUrl='" + streamUrl + '\'' +
                ", status=" + status +
                ", lastOnlineTime='" + lastOnlineTime + '\'' +
                ", installTime='" + installTime + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}