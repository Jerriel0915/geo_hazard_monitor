package com.zwei.iot.timeseries.domain;

import com.zwei.iot.device.domain.SensorAttribute;
import lombok.Builder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 传感器时序上下文。
 *
 * <p>聚合设备、传感器与属性元数据，用于在报文解析阶段补齐指标中文名称、单位等
 * 上下文信息，确保写入 IoTDB 的数据携带完整的业务语义。</p>
 *
 * <p>字段说明：</p>
 * <ul>
 *   <li>{@code deviceId} — 设备ID</li>
 *   <li>{@code sensorId} — 传感器主键ID</li>
 *   <li>{@code attributes} — 传感器关联的指标属性列表</li>
 * </ul>
 */
@Builder
public record SensorMetadata(
        Long deviceId,
        Long sensorId,
        List<SensorAttribute> attributes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
