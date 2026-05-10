package com.zwei.common.core.domain.model;

import java.util.Date;
import java.util.List;

/**
 * 用户信息响应DTO
 * 字段映射说明：
 * - nickName → realName (文档字段名)
 * - userId → id (文档字段名)
 * - userName → username (文档字段名)
 * - deptId → orgId (文档字段名)
 * - loginDate → lastLoginTime (文档字段名)
 * - createBy → creator (文档字段名)
 * - updateBy → updater (文档字段名)
 *
 * @author zwei
 */
public class SysUserResponse
{
    /** 用户ID */
    private Long id;

    /** 用户账号 */
    private String username;

    /** 用户昵称（映射为文档中的realName） */
    private String realName;

    /** 用户头像 */
    private String avatar;

    /** 手机号码 */
    private String phone;

    /** 用户邮箱 */
    private String email;

    /** 部门ID（映射为文档中的orgId） */
    private Long orgId;

    /** 部门名称（映射为文档中的orgName） */
    private String orgName;

    /** 账号状态（0正常 1停用） */
    private Integer status;

    /** 最后登录时间（映射为文档中的lastLoginTime） */
    private Date lastLoginTime;

    /** 创建时间 */
    private Date createTime;

    /**
     * 创建者
     */
    private String createBy;

    /** 更新时间 */
    private Date updateTime;

    /**
     * 更新者
     */
    private String updateBy;

    /** 备注 */
    private String remark;

    /** 角色ID列表 */
    private List<Long> roleIds;

    /** 岗位ID列表 */
    private List<Long> postIds;

    /** 角色列表 */
    private List<SysRoleResponse> roles;

    /** 岗位列表 */
    private List<SysPostResponse> posts;

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

    public Date getLastLoginTime()
    {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime)
    {
        this.lastLoginTime = lastLoginTime;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public List<Long> getRoleIds()
    {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds)
    {
        this.roleIds = roleIds;
    }

    public List<Long> getPostIds()
    {
        return postIds;
    }

    public void setPostIds(List<Long> postIds)
    {
        this.postIds = postIds;
    }

    public List<SysRoleResponse> getRoles()
    {
        return roles;
    }

    public void setRoles(List<SysRoleResponse> roles)
    {
        this.roles = roles;
    }

    public List<SysPostResponse> getPosts()
    {
        return posts;
    }

    public void setPosts(List<SysPostResponse> posts)
    {
        this.posts = posts;
    }

    /**
     * 角色响应DTO
     */
    public static class SysRoleResponse
    {
        private Long roleId;
        private String roleName;
        private String roleKey;

        public Long getRoleId()
        {
            return roleId;
        }

        public void setRoleId(Long roleId)
        {
            this.roleId = roleId;
        }

        public String getRoleName()
        {
            return roleName;
        }

        public void setRoleName(String roleName)
        {
            this.roleName = roleName;
        }

        public String getRoleKey()
        {
            return roleKey;
        }

        public void setRoleKey(String roleKey)
        {
            this.roleKey = roleKey;
        }
    }

    /**
     * 从SysUser实体映射为响应DTO
     * 注意：nickName映射为realName，userId映射为id，userName映射为username
     * deptId映射为orgId，loginDate映射为lastLoginTime
     */
    public static SysUserResponse fromEntity(com.zwei.common.core.domain.entity.SysUser user)
    {
        if (user == null)
        {
            return null;
        }
        SysUserResponse resp = new SysUserResponse();
        resp.setId(user.getUserId());
        resp.setUsername(user.getUserName());
        resp.setRealName(user.getNickName()); // nickName映射为文档中的realName
        resp.setAvatar(user.getAvatar());
        resp.setPhone(user.getPhonenumber());
        resp.setEmail(user.getEmail());
        resp.setOrgId(user.getDeptId());
        if (user.getDept() != null)
        {
            resp.setOrgName(user.getDept().getDeptName());
        }
        // status in entity is String "0"/"1", convert to Integer
        if (user.getStatus() != null)
        {
            resp.setStatus(Integer.valueOf(user.getStatus()));
        }
        resp.setLastLoginTime(user.getLoginDate());
        resp.setCreateTime(user.getCreateTime());
        resp.setCreateBy(user.getCreateBy());
        resp.setUpdateTime(user.getUpdateTime());
        resp.setUpdateBy(user.getUpdateBy());
        resp.setRemark(user.getRemark());
        return resp;
    }

    /**
     * 岗位响应DTO
     */
    public static class SysPostResponse
    {
        private Long postId;
        private String postName;
        private String postCode;

        public Long getPostId()
        {
            return postId;
        }

        public void setPostId(Long postId)
        {
            this.postId = postId;
        }

        public String getPostName()
        {
            return postName;
        }

        public void setPostName(String postName)
        {
            this.postName = postName;
        }

        public String getPostCode()
        {
            return postCode;
        }

        public void setPostCode(String postCode)
        {
            this.postCode = postCode;
        }
    }
}
