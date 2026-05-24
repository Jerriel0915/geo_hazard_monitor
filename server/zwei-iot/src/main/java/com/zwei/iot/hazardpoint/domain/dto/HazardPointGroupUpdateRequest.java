package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 修改隐患点分组请求参数
 */
@Setter
@Getter
public class HazardPointGroupUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "分组编码长度不能超过100个字符")
    private String code;

    @Size(max = 200, message = "分组名称长度不能超过200个字符")
    private String name;

    @Size(max = 500, message = "分组描述长度不能超过500个字符")
    private String description;

    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "排序号超出范围")
    private Integer sortOrder;

    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}
