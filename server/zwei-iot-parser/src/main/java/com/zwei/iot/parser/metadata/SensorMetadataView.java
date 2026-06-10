package com.zwei.iot.parser.metadata;

import java.util.List;

/**
 * 传感器元数据视图。
 *
 * <p>定义 parser 模块所需的传感器元数据最小集合，解耦对 zwei-iot-device 模块的编译时依赖。
 * 由 device 模块的 {@code SensorMetadata} record 实现。
 */
public interface SensorMetadataView {

    Long deviceId();

    Long sensorId();

    List<? extends SensorAttributeView> attributes();
}
