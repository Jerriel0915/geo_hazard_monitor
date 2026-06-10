package com.zwei.iot.parser.metadata;

/**
 * 传感器属性元数据视图。
 *
 * <p>定义 parser 模块所需的最小属性信息，解耦对 zwei-iot-device 模块的编译时依赖。
 * 由 device 模块的 {@code SensorAttribute} 实现。
 */
public interface SensorAttributeView {

    String getAttrCode();

    String getAttrName();

    String getUnit();
}
