package com.zwei.common.core.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.zwei.common.core.domain.entity.SysUser;

/**
 * 个人中心资料修改请求 DTO
 */
public class SysProfileUpdateRequest
{
    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 30, message = "真实姓名长度不能超过30个字符")
    private String realName;

    private String phone;

    private String email;

    private String sex;

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public void applyTo(SysUser user)
    {
        user.setNickName(realName);
        user.setPhonenumber(phone);
        user.setEmail(email);
        user.setSex(sex);
    }
}
