package com.zwei.iot.monitor.domain;

import com.zwei.common.core.domain.BaseEntity;

import java.io.Serial;

/**
 * 监测类型表 monitor_type
 * <p>
 * 监测类型用于定义不同的监测方式，如雨量监测、水位监测、位移监测等。
 * 每个监测类型可以包含多个监测内容。
 *
 * @author zwei
 */
public class MonitorType extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 监测类型编码
     */
    private String code;

    /**
     * 监测类型名称
     */
    private String name;

    /**
     * 设备类型: 1-直连设备, 2-传感器, 3-RTU
     */
    private Integer deviceType;

    /**
     * 设备类型名称（查询时返回）
     */
    private String deviceTypeName;

    /**
     * 图标路径
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 监测内容列表（查询详情时返回）
     */
    private Object contents;

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

    public Integer getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(Integer deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceTypeName() {
        return deviceTypeName;
    }

    public void setDeviceTypeName(String deviceTypeName) {
        this.deviceTypeName = deviceTypeName;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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

    public Object getContents() {
        return contents;
    }

    public void setContents(Object contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "MonitorType{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", deviceType=" + deviceType +
                ", deviceTypeName='" + deviceTypeName + '\'' +
                ", icon='" + icon + '\'' +
                ", description='" + description + '\'' +
                ", sortOrder=" + sortOrder +
                ", status=" + status +
                ", delFlag=" + delFlag +
                '}';
    }
}