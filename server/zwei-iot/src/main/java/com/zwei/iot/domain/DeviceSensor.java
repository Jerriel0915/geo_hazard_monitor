package com.zwei.iot.domain;

import com.zwei.common.core.domain.BaseEntity;

import java.io.Serial;
import java.util.List;

/**
 * 传感器表 device_sensor
 *
 * @author zwei
 */
public class DeviceSensor extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 传感器编号
     */
    private String sensorCode;

    /**
     * 传感器名称
     */
    private String sensorName;

    /**
     * 监测类型ID
     */
    private Long monitorTypeId;

    /**
     * 监测类型编码
     */
    private String monitorTypeCode;

    /**
     * 监测类型名称
     */
    private String monitorTypeName;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 属性列表（查询详情时返回）
     */
    private List<SensorAttribute> attrList;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getSensorCode() {
        return sensorCode;
    }

    public void setSensorCode(String sensorCode) {
        this.sensorCode = sensorCode;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public Long getMonitorTypeId() {
        return monitorTypeId;
    }

    public void setMonitorTypeId(Long monitorTypeId) {
        this.monitorTypeId = monitorTypeId;
    }

    public String getMonitorTypeCode() {
        return monitorTypeCode;
    }

    public void setMonitorTypeCode(String monitorTypeCode) {
        this.monitorTypeCode = monitorTypeCode;
    }

    public String getMonitorTypeName() {
        return monitorTypeName;
    }

    public void setMonitorTypeName(String monitorTypeName) {
        this.monitorTypeName = monitorTypeName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public List<SensorAttribute> getAttrList() {
        return attrList;
    }

    public void setAttrList(List<SensorAttribute> attrList) {
        this.attrList = attrList;
    }

    @Override
    public String toString() {
        return "DeviceSensor{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", deviceCode='" + deviceCode + '\'' +
                ", sensorCode='" + sensorCode + '\'' +
                ", sensorName='" + sensorName + '\'' +
                ", monitorTypeId=" + monitorTypeId +
                ", monitorTypeCode='" + monitorTypeCode + '\'' +
                ", monitorTypeName='" + monitorTypeName + '\'' +
                ", status=" + status +
                ", delFlag=" + delFlag +
                '}';
    }
}