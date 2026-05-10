package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysMenu;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单信息响应DTO
 * 字段映射说明：
 * - menuId → id
 * - menuName → name
 * - menuType → type
 * - orderNum → sortOrder
 * - perms → code
 *
 * @author zwei
 */
public class SysMenuResponse {
    /**
     * 菜单ID
     */
    private Long id;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单编码
     */
    private String code;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 类型（M目录 C菜单 F按钮）
     */
    private Integer type;

    /**
     * 显示状态（0显示 1隐藏）
     */
    private Integer visible;

    /**
     * 是否缓存（0缓存 1不缓存）
     */
    private Integer isCache;

    /**
     * 显示顺序
     */
    private Integer sortOrder;

    /**
     * 权限字符串
     */
    private String perms;

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

    /**
     * 子菜单
     */
    private List<SysMenuResponse> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getIsCache() {
        return isCache;
    }

    public void setIsCache(Integer isCache) {
        this.isCache = isCache;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
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

    public List<SysMenuResponse> getChildren() {
        return children;
    }

    public void setChildren(List<SysMenuResponse> children) {
        this.children = children;
    }

    /**
     * 从SysMenu实体映射为响应DTO
     */
    public static SysMenuResponse fromEntity(SysMenu menu) {
        if (menu == null) {
            return null;
        }
        SysMenuResponse resp = new SysMenuResponse();
        resp.setId(menu.getMenuId());
        resp.setParentId(menu.getParentId());
        resp.setName(menu.getMenuName());
        resp.setCode(menu.getPerms());
        resp.setPath(menu.getPath());
        resp.setComponent(menu.getComponent());
        resp.setIcon(menu.getIcon());
        // menuType: M=目录(0), C=菜单(1), F=按钮(2)
        if (menu.getMenuType() != null) {
            switch (menu.getMenuType()) {
                case "M":
                    resp.setType(0);
                    break;
                case "C":
                    resp.setType(1);
                    break;
                case "F":
                    resp.setType(2);
                    break;
                default:
                    resp.setType(null);
            }
        }
        resp.setVisible(menu.getVisible() != null ? Integer.valueOf(menu.getVisible()) : null);
        resp.setIsCache(menu.getIsCache() != null ? Integer.valueOf(menu.getIsCache()) : null);
        resp.setSortOrder(menu.getOrderNum());
        resp.setPerms(menu.getPerms());
        resp.setCreateTime(menu.getCreateTime());
        resp.setCreateBy(menu.getCreateBy());
        resp.setUpdateTime(menu.getUpdateTime());
        resp.setUpdateBy(menu.getUpdateBy());
        return resp;
    }

    /**
     * 将菜单列表递归映射为树形结构
     */
    public static List<SysMenuResponse> buildTree(List<SysMenu> menus) {
        if (menus == null) {
            return null;
        }
        return menus.stream()
                .map(SysMenuResponse::fromEntity)
                .peek(resp -> {
                    List<SysMenu> children = menus.stream()
                            .filter(m -> m.getParentId() != null && m.getParentId().equals(resp.getId()))
                            .collect(Collectors.toList());
                    if (!children.isEmpty()) {
                        resp.setChildren(buildTree(children));
                    }
                })
                .collect(Collectors.toList());
    }
}