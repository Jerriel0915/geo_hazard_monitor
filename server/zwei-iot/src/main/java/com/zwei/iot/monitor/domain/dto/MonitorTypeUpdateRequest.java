package com.zwei.iot.monitor.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改监测类型请求参数
 */
@Setter
@Getter
public class MonitorTypeUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 200, message = "监测类型名称长度不能超过200个字符")
    private String name;

    @Min(value = 1, message = "设备类型值不合法")
    @Max(value = 3, message = "设备类型值不合法")
    private Integer deviceType;

    @Size(max = 200, message = "图标路径长度不能超过200个字符")
    private String icon;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "排序号超出范围")
    private Integer sortOrder;

    public boolean hasUpdatableField() {
        return name != null
                || deviceType != null
                || icon != null
                || description != null
                || sortOrder != null;
    }
}
