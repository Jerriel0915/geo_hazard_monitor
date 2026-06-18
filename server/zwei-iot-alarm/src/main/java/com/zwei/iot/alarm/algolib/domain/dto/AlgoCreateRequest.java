package com.zwei.iot.alarm.algolib.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 算法 — 新增请求。
 *
 * @author zwei
 */
public class AlgoCreateRequest {

    @NotBlank(message = "算法编码不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,63}$",
            message = "算法编码必须以大写字母开头，3-64 字符，仅含大写字母/数字/下划线")
    private String code;

    @NotBlank(message = "算法名称不能为空")
    @Size(max = 128, message = "算法名称不能超过 128 字符")
    private String name;

    @Size(max = 500, message = "算法描述不能超过 500 字符")
    private String description;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
