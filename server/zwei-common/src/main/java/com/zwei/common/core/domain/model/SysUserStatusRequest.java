package com.zwei.common.core.domain.model;

import jakarta.validation.constraints.NotNull;

/**
 * 用户状态修改请求 — 窄 DTO，防止 mass-assignment 绑定 SysUser 整域。
 */
public class SysUserStatusRequest
{
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "状态不能为空")
    private String status;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
