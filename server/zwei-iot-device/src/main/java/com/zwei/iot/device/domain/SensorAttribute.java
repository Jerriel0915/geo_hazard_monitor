package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 传感器属性表 sensor_attribute
 *
 * @author zwei
 */
@Setter
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SensorAttribute extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 传感器ID
     */
    private Long sensorId;
    private Long monitorContentId;

    /**
     * 属性编码
     */
    private String attrCode;

    /**
     * 属性名称
     */
    private String attrName;

    /**
     * 初始值
     */
    private BigDecimal initialValue;

    /**
     * 单位
     */
    private String unit;

    /**
     * 最小值范围
     */
    private BigDecimal rangeMin;

    /**
     * 最大值范围
     */
    private BigDecimal rangeMax;

    /**
     * 图标路径
     */
    private String icon;

    @Override
    public String toString() {
        return "SensorAttribute{" +
                "id=" + id +
                ", sensorId=" + sensorId +
                ", attrCode='" + attrCode + '\'' +
                ", attrName='" + attrName + '\'' +
                ", initialValue=" + initialValue +
                ", unit='" + unit + '\'' +
                ", rangeMin=" + rangeMin +
                ", rangeMax=" + rangeMax +
                ", icon='" + icon + '\'' +
                '}';
    }
}