package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysDept;

/**
 * 组织查询请求
 */
public class SysOrganizationQueryRequest
{
    private String code;

    private String name;

    private Integer status;

    public SysDept toEntity()
    {
        SysDept dept = new SysDept();
        dept.setCode(code);
        dept.setDeptName(name);
        dept.setStatus(status == null ? null : String.valueOf(status));
        return dept;
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
