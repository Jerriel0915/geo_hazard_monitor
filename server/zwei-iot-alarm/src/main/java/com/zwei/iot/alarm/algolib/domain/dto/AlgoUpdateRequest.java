package com.zwei.iot.alarm.algolib.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 算法 — 修改请求（不可修改 code）。
 *
 * @author zwei
 */
public class AlgoUpdateRequest {

    @NotBlank(message = "算法名称不能为空")
    @Size(max = 128, message = "算法名称不能超过 128 字符")
    private String name;

    @Size(max = 500, message = "算法描述不能超过 500 字符")
    private String description;

    @Size(max = 500, message = "备注不能超过 500 字符")
    private String remark;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
