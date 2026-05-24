package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysUser;

/**
 * 用户列表查询请求
 */
public class SysUserQueryRequest
{
    private String username;

    private String realName;

    private Long orgId;

    private Integer status;

    public SysUser toEntity()
    {
        SysUser user = new SysUser();
        user.setUserName(username);
        user.setNickName(realName);
        user.setDeptId(orgId);
        user.setStatus(status == null ? null : String.valueOf(status));
        return user;
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
}
