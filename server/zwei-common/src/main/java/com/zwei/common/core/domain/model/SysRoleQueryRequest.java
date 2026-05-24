package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysRole;

/**
 * 角色列表查询请求
 */
public class SysRoleQueryRequest
{
    private String code;

    private String name;

    private Integer status;

    public SysRole toEntity()
    {
        SysRole role = new SysRole();
        role.setRoleKey(code);
        role.setRoleName(name);
        role.setStatus(status == null ? null : String.valueOf(status));
        return role;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
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
