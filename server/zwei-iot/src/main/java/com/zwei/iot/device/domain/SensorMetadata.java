package com.zwei.iot.device.domain;

import lombok.Builder;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Builder
public record SensorMetadata(
        Long deviceId,
        Long sensorId,
        List<SensorAttribute> attributes
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
