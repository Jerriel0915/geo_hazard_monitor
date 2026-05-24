package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysRole;

import java.util.Date;
import java.util.List;

/**
 * 角色信息响应DTO
 * 字段映射说明：
 * - roleId → id
 * - roleKey → code
 * - roleName → name
 * - roleSort → sortOrder
 * - remark → description
 *
 * @author zwei
 */
public class SysRoleResponse {
    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 数据范围
     */
    private Integer dataScope;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 角色状态（0正常 1停用）
     */
    private Integer status;

    /**
     * 菜单ID列表
     */
    private List<Long> menuIds;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 更新者
     */
    private String updateBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDataScope() {
        return dataScope;
    }

    public void setDataScope(Integer dataScope) {
        this.dataScope = dataScope;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    /**
     * 从SysRole实体映射为响应DTO
     */
    public static SysRoleResponse fromEntity(SysRole role) {
        if (role == null) {
            return null;
        }
        SysRoleResponse resp = new SysRoleResponse();
        resp.setId(role.getRoleId());
        resp.setCode(role.getRoleKey());
        resp.setName(role.getRoleName());
        resp.setDescription(role.getRemark());
        resp.setDataScope(role.getDataScope() == null ? null : Integer.valueOf(role.getDataScope()));
        resp.setSortOrder(role.getRoleSort());
        if (role.getStatus() != null) {
            resp.setStatus(Integer.valueOf(role.getStatus()));
        }
        resp.setCreateTime(role.getCreateTime());
        resp.setCreateBy(role.getCreateBy());
        resp.setUpdateTime(role.getUpdateTime());
        resp.setUpdateBy(role.getUpdateBy());
        return resp;
    }
}
