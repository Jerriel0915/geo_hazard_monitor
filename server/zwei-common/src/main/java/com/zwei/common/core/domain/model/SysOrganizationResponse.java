package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysDept;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组织响应DTO
 */
public class SysOrganizationResponse
{
    private Long id;

    private String code;

    private String name;

    private Long parentId;

    private String parentIds;

    private Integer level;

    private String leader;

    private String phone;

    private String email;

    private String region;

    private String address;

    private Integer status;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    private List<SysOrganizationResponse> children;

    public static SysOrganizationResponse fromEntity(SysDept dept)
    {
        if (dept == null)
        {
            return null;
        }
        SysOrganizationResponse response = new SysOrganizationResponse();
        response.setId(dept.getDeptId());
        response.setCode(dept.getCode());
        response.setName(dept.getDeptName());
        response.setParentId(dept.getParentId());
        response.setParentIds(dept.getParentIds());
        response.setLevel(dept.getLevel());
        response.setLeader(dept.getLeader());
        response.setPhone(dept.getPhone());
        response.setEmail(dept.getEmail());
        response.setRegion(dept.getRegion());
        response.setAddress(dept.getAddress());
        response.setStatus(dept.getStatus() == null ? null : Integer.valueOf(dept.getStatus()));
        response.setSortOrder(dept.getOrderNum());
        response.setCreateTime(dept.getCreateTime());
        response.setUpdateTime(dept.getUpdateTime());
        if (dept.getChildren() != null && !dept.getChildren().isEmpty())
        {
            response.setChildren(dept.getChildren().stream().map(SysOrganizationResponse::fromEntity).collect(Collectors.toList()));
        }
        return response;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
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

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getParentIds()
    {
        return parentIds;
    }

    public void setParentIds(String parentIds)
    {
        this.parentIds = parentIds;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public String getLeader()
    {
        return leader;
    }

    public void setLeader(String leader)
    {
        this.leader = leader;
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

    public String getRegion()
    {
        return region;
    }

    public void setRegion(String region)
    {
        this.region = region;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public List<SysOrganizationResponse> getChildren()
    {
        return children;
    }

    public void setChildren(List<SysOrganizationResponse> children)
    {
        this.children = children;
    }
}
