package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysDept;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 组织新增/修改请求
 */
public class SysOrganizationUpsertRequest
{
    @Size(max = 64, message = "组织编码长度不能超过64个字符")
    private String code;

    @Size(max = 30, message = "组织名称长度不能超过30个字符")
    private String name;

    @NotNull(message = "父组织不能为空")
    private Long parentId;

    @Size(max = 20, message = "负责人长度不能超过20个字符")
    private String leader;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过50个字符")
    private String email;

    @Size(max = 50, message = "区域长度不能超过50个字符")
    private String region;

    @Size(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    private Integer sortOrder;

    private Integer status;

    public SysDept toEntity()
    {
        SysDept dept = new SysDept();
        dept.setCode(code);
        dept.setDeptName(name);
        dept.setParentId(parentId);
        dept.setLeader(leader);
        dept.setPhone(phone);
        dept.setEmail(email);
        dept.setRegion(region);
        dept.setAddress(address);
        dept.setOrderNum(sortOrder);
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

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
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
}
