package com.zwei.iot.monitor.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增监测类型请求参数
 */
@Setter
@Getter
public class MonitorTypeCreateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "监测类型编码不能为空")
    @Size(max = 100, message = "监测类型编码长度不能超过100个字符")
    private String code;
    @Min(value = 1, message = "监测大类ID不合法")
    private Long categoryId;

    @NotBlank(message = "监测类型名称不能为空")
    @Size(max = 200, message = "监测类型名称长度不能超过200个字符")
    private String name;

    @Size(max = 200, message = "图标路径长度不能超过200个字符")
    private String icon;

    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "排序号超出范围")
    private Integer sortOrder;

    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}
