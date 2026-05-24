package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysUser;

/**
 * 个人中心信息响应 DTO
 */
public class SysProfileResponse
{
    private Long id;

    private String username;

    private String realName;

    private String avatar;

    private String phone;

    private String email;

    private Long orgId;

    private String orgName;

    private Integer status;

    private String sex;

    private String roleGroup;

    private String postGroup;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
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

    public String getOrgName()
    {
        return orgName;
    }

    public void setOrgName(String orgName)
    {
        this.orgName = orgName;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public String getRoleGroup()
    {
        return roleGroup;
    }

    public void setRoleGroup(String roleGroup)
    {
        this.roleGroup = roleGroup;
    }

    public String getPostGroup()
    {
        return postGroup;
    }

    public void setPostGroup(String postGroup)
    {
        this.postGroup = postGroup;
    }

    public static SysProfileResponse fromUser(SysUser user, String roleGroup, String postGroup)
    {
        SysProfileResponse response = new SysProfileResponse();
        response.setId(user.getUserId());
        response.setUsername(user.getUserName());
        response.setRealName(user.getNickName());
        response.setAvatar(user.getAvatar());
        response.setPhone(user.getPhonenumber());
        response.setEmail(user.getEmail());
        response.setOrgId(user.getDeptId());
        if (user.getDept() != null)
        {
            response.setOrgName(user.getDept().getDeptName());
        }
        if (user.getStatus() != null)
        {
            response.setStatus(Integer.valueOf(user.getStatus()));
        }
        response.setSex(user.getSex());
        response.setRoleGroup(roleGroup);
        response.setPostGroup(postGroup);
        return response;
    }
}
