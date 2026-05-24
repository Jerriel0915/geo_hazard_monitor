package com.zwei.common.core.domain.model;

import com.zwei.common.core.domain.entity.SysMenu;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 菜单新增/修改请求
 */
public class SysMenuUpsertRequest
{
    @NotNull(message = "父级菜单不能为空")
    private Long parentId;

    @Size(max = 50, message = "菜单名称长度不能超过50个字符")
    private String name;

    @Size(max = 100, message = "菜单编码长度不能超过100个字符")
    private String code;

    @Size(max = 200, message = "路由地址不能超过200个字符")
    private String path;

    @Size(max = 255, message = "组件路径长度不能超过255个字符")
    private String component;

    @Size(max = 100, message = "权限标识长度不能超过100个字符")
    private String perms;

    @Size(max = 100, message = "图标长度不能超过100个字符")
    private String icon;

    private Integer type;

    private Integer visible;

    private Integer isCache;

    private Integer sortOrder;

    private Integer status;

    public SysMenu toEntity()
    {
        SysMenu menu = new SysMenu();
        applyTo(menu, true);
        return menu;
    }

    public void applyTo(SysMenu menu, boolean overwriteNull)
    {
        if (overwriteNull || name != null)
        {
            menu.setMenuName(name);
        }
        if (overwriteNull || parentId != null)
        {
            menu.setParentId(parentId);
        }
        if (overwriteNull || code != null)
        {
            menu.setRouteName(code);
        }
        if (overwriteNull || path != null)
        {
            menu.setPath(path);
        }
        if (overwriteNull || component != null)
        {
            menu.setComponent(component);
        }
        if (overwriteNull || perms != null)
        {
            menu.setPerms(perms);
        }
        if (overwriteNull || icon != null)
        {
            menu.setIcon(icon);
        }
        if (overwriteNull || type != null)
        {
            menu.setMenuType(toMenuType(type));
        }
        if (overwriteNull || visible != null)
        {
            menu.setVisible(visible == null ? null : String.valueOf(visible));
        }
        if (overwriteNull || isCache != null)
        {
            menu.setIsCache(isCache == null ? null : String.valueOf(isCache));
        }
        if (overwriteNull || sortOrder != null)
        {
            menu.setOrderNum(sortOrder);
        }
        if (overwriteNull || status != null)
        {
            menu.setStatus(status == null ? null : String.valueOf(status));
        }

        if (menu.getIsFrame() == null)
        {
            menu.setIsFrame("1");
        }
        if (menu.getVisible() == null)
        {
            menu.setVisible("0");
        }
        if (menu.getIsCache() == null)
        {
            menu.setIsCache("1");
        }
        if (menu.getStatus() == null)
        {
            menu.setStatus("0");
        }
    }

    private String toMenuType(Integer type)
    {
        if (type == null)
        {
            return null;
        }
        switch (type)
        {
            case 0:
                return "M";
            case 1:
                return "C";
            case 2:
                return "F";
            default:
                return null;
        }
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getComponent()
    {
        return component;
    }

    public void setComponent(String component)
    {
        this.component = component;
    }

    public String getPerms()
    {
        return perms;
    }

    public void setPerms(String perms)
    {
        this.perms = perms;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public Integer getType()
    {
        return type;
    }

    public void setType(Integer type)
    {
        this.type = type;
    }

    public Integer getVisible()
    {
        return visible;
    }

    public void setVisible(Integer visible)
    {
        this.visible = visible;
    }

    public Integer getIsCache()
    {
        return isCache;
    }

    public void setIsCache(Integer isCache)
    {
        this.isCache = isCache;
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
