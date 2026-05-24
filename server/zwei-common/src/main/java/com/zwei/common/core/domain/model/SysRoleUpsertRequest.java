package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysRole;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 角色新增/修改请求
 */
public class SysRoleUpsertRequest
{
    @Size(max = 100, message = "角色编码长度不能超过100个字符")
    private String code;

    @Size(max = 30, message = "角色名称长度不能超过30个字符")
    private String name;

    @Size(max = 500, message = "角色描述长度不能超过500个字符")
    private String description;

    private Integer dataScope;

    private Integer sortOrder;

    private Integer status;

    private List<Long> menuIds;

    private List<Long> deptIds;

    public SysRole toEntity()
    {
        SysRole role = new SysRole();
        applyTo(role, true);
        return role;
    }

    public void applyTo(SysRole role, boolean includeCode)
    {
        if (includeCode)
        {
            role.setRoleKey(code);
        }
        role.setRoleName(name);
        role.setRemark(description);
        role.setDataScope(dataScope == null ? null : String.valueOf(dataScope));
        role.setRoleSort(sortOrder);
        role.setStatus(status == null ? null : String.valueOf(status));
        role.setMenuIds(menuIds == null ? null : menuIds.toArray(new Long[0]));
        role.setDeptIds(deptIds == null ? null : deptIds.toArray(new Long[0]));
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Integer getDataScope()
    {
        return dataScope;
    }

    public void setDataScope(Integer dataScope)
    {
        this.dataScope = dataScope;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public List<Long> getMenuIds()
    {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds)
    {
        this.menuIds = menuIds;
    }

    public List<Long> getDeptIds()
    {
        return deptIds;
    }

    public void setDeptIds(List<Long> deptIds)
    {
        this.deptIds = deptIds;
    }
}
