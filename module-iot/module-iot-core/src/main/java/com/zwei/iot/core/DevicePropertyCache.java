package com.zwei.iot.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevicePropertyCache {

    /**
     * 属性值
     */
    private Object value;

    /**
     * 属性值时间: 设备上报时间
     */
    private Long occurred;

}
