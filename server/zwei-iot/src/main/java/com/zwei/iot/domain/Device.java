package com.zwei.iot.domain;

import com.zwei.common.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 设备表 device
 *
 * @author zwei
 */
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
     * 设备图标
     */
    private String icon;

    /**
     * 图标路径
     */
    private String iconPath;

    /**
     * 状态: 1-正常, 2-故障, 3-离线
     */
    private Integer status;

    /**
     * 状态名称（查询时返回）
     */
    private String statusName;

    /**
     * 运行状态: 0-未知, 1-运行中, 2-停止
     */
    private Integer runStatus;

    /**
     * 运行状态名称（查询时返回）
     */
    private String runStatusName;

    /**
     * 最近上报时间
     */
    private String lastReportTime;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 传感器列表（查询详情时返回）
     */
    private Object sensors;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(Integer runStatus) {
        this.runStatus = runStatus;
    }

    public String getRunStatusName() {
        return runStatusName;
    }

    public void setRunStatusName(String runStatusName) {
        this.runStatusName = runStatusName;
    }

    public String getLastReportTime() {
        return lastReportTime;
    }

    public void setLastReportTime(String lastReportTime) {
        this.lastReportTime = lastReportTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public Object getSensors() {
        return sensors;
    }

    public void setSensors(Object sensors) {
        this.sensors = sensors;
    }

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", iconPath='" + iconPath + '\'' +
                ", status=" + status +
                ", statusName='" + statusName + '\'' +
                ", runStatus=" + runStatus +
                ", runStatusName='" + runStatusName + '\'' +
                ", lastReportTime='" + lastReportTime + '\'' +
                ", delFlag=" + delFlag +
                '}';
    }
}