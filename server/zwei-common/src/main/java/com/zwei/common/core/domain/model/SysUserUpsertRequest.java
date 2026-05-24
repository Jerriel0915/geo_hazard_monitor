package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 用户新增/修改请求
 */
public class SysUserUpsertRequest
{
    @Size(max = 30, message = "用户名长度不能超过30个字符")
    private String username;

    @Size(max = 50, message = "密码长度不能超过50个字符")
    private String password;

    @Size(max = 30, message = "真实姓名长度不能超过30个字符")
    private String realName;

    @Size(max = 11, message = "手机号长度不能超过11个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    private Long orgId;

    private Integer status;

    private List<Long> roleIds;

    private String remark;

    public SysUser toEntity()
    {
        SysUser user = new SysUser();
        applyTo(user, true);
        return user;
    }

    public void applyTo(SysUser user, boolean includeUsername)
    {
        if (includeUsername)
        {
            user.setUserName(username);
        }
        if (password != null)
        {
            user.setPassword(password);
        }
        user.setNickName(realName);
        user.setPhonenumber(phone);
        user.setEmail(email);
        user.setDeptId(orgId);
        user.setStatus(status == null ? null : String.valueOf(status));
        user.setRoleIds(roleIds == null ? null : roleIds.toArray(new Long[0]));
        user.setRemark(remark);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

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

    public Long getOrgId()
    {
        return orgId;
    }

    public void setOrgId(Long orgId)
    {
        this.orgId = orgId;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public List<Long> getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds)
    {
        this.roleIds = roleIds;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
