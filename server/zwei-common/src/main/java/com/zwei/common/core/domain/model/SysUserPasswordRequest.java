package com.zwei.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户密码修改请求
 */
public class SysUserPasswordRequest
{
    @NotBlank(message = "旧密码不能为空")
    @Size(max = 50, message = "旧密码长度不能超过50个字符")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 50, message = "新密码长度不能超过50个字符")
    private String newPassword;

    public String getOldPassword()
    {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword)
    {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }
}
