package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * 传感器表 device_sensor
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
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
     * 埋深(米)，地面为0点，向下为正，向上为负
     */
    private BigDecimal burialDepth;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 删除标记: 0-正常, 1-删除
     */
    private Integer delFlag;

    /**
     * 监测类型图标路径 (来自 monitor_type.icon, 非持久化字段, 仅查询时 JOIN 填充)
     * <p>供前端 getSensorIconPath() 解析, 缺失时前端按 monitorTypeName 兜底。</p>
     */
    private String iconPath;

    /**
     * 属性列表（查询详情时返回）
     */
    private List<SensorAttribute> attrList;

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
                ", burialDepth=" + burialDepth +
                ", status=" + status +
                ", delFlag=" + delFlag +
                ", iconPath='" + iconPath + '\'' +
                '}';
    }
}
