package com.zwei.iot.timeseries.domain;

import java.io.Serializable;

/**
 * 监测数据统一响应对象。
 *
 * <p>封装单条监测数据的完整业务信息，供 {@code MonitorDataController} 的三个查询接口统一使用。</p>
 */
public record MonitorDataVO(
        Long hazardPointId,
        String hazardPointName,
        Long deviceId,
        String deviceName,
        Long sensorId,
        String sensorName,
        String attrCode,
        String attrName,
        Double value,
        String unit,
        String dataTime,
        Integer quality,
        String qualityText
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
