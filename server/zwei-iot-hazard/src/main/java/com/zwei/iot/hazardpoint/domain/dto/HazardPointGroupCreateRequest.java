package com.zwei.iot.hazardpoint.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增隐患点分组请求参数
 */
@Setter
@Getter
public class HazardPointGroupCreateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分组编码不能为空")
    @Size(max = 100, message = "分组编码长度不能超过100个字符")
    private String code;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 200, message = "分组名称长度不能超过200个字符")
    private String name;

    @Size(max = 500, message = "分组描述长度不能超过500个字符")
    private String description;

    @NotNull(message = "排序号不能为空")
    @Min(value = 0, message = "排序号不能小于0")
    @Max(value = Integer.MAX_VALUE, message = "排序号超出范围")
    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值不合法")
    @Max(value = 1, message = "状态值不合法")
    private Integer status;
}
