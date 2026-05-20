package com.zwei.iot.device.domain;

import com.zwei.common.core.domain.BaseEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 传感器属性表 sensor_attribute
 *
 * @author zwei
 */
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

    /**
     * 属性编码
     */
    private String attrCode;

    /**
     * 属性名称
     */
    private String attrName;

    /**
     * 指标类型
     */
    private String indicatorType;

    /**
     * 指标类型名称
     */
    private String indicatorTypeName;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSensorId() {
        return sensorId;
    }

    public void setSensorId(Long sensorId) {
        this.sensorId = sensorId;
    }

    public String getAttrCode() {
        return attrCode;
    }

    public void setAttrCode(String attrCode) {
        this.attrCode = attrCode;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public String getIndicatorType() {
        return indicatorType;
    }

    public void setIndicatorType(String indicatorType) {
        this.indicatorType = indicatorType;
    }

    public String getIndicatorTypeName() {
        return indicatorTypeName;
    }

    public void setIndicatorTypeName(String indicatorTypeName) {
        this.indicatorTypeName = indicatorTypeName;
    }

    public BigDecimal getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(BigDecimal initialValue) {
        this.initialValue = initialValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getRangeMin() {
        return rangeMin;
    }

    public void setRangeMin(BigDecimal rangeMin) {
        this.rangeMin = rangeMin;
    }

    public BigDecimal getRangeMax() {
        return rangeMax;
    }

    public void setRangeMax(BigDecimal rangeMax) {
        this.rangeMax = rangeMax;
    }

    @Override
    public String toString() {
        return "SensorAttribute{" +
                "id=" + id +
                ", sensorId=" + sensorId +
                ", attrCode='" + attrCode + '\'' +
                ", attrName='" + attrName + '\'' +
                ", indicatorType='" + indicatorType + '\'' +
                ", indicatorTypeName='" + indicatorTypeName + '\'' +
                ", initialValue=" + initialValue +
                ", unit='" + unit + '\'' +
                ", rangeMin=" + rangeMin +
                ", rangeMax=" + rangeMax +
                '}';
    }
}