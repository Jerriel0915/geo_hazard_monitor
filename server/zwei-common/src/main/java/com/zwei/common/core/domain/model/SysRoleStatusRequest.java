package com.zwei.common.core.domain.model;

import jakarta.validation.constraints.NotNull;

/**
 * 角色状态修改请求 — 窄 DTO，防止 mass-assignment 绑定 SysRole 整域。
 * 角色 ID 从路径参数 {@code {id}} 获取，请求体仅需 {@code status}。
 */
public class SysRoleStatusRequest
{
    @NotNull(message = "状态不能为空")
    private String status;

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
