package com.zwei.common.core.domain.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

/**
 * 菜单批量重排请求项
 *
 * <p>配合 POST /api/v1/menus/reorder 使用，一次事务内批量更新
 * 多个菜单的 parentId 与 orderNum。
 */
public class SysMenuReorderItem implements Serializable
{
    private static final long serialVersionUID = 1L;

    @NotNull(message = "菜单ID不能为空")
    private Long menuId;

    @NotNull(message = "父级菜单ID不能为空")
    private Long parentId;

    @NotNull(message = "排序号不能为空")
    private Integer orderNum;

    public Long getMenuId()
    {
        return menuId;
    }

    public void setMenuId(Long menuId)
    {
        this.menuId = menuId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }
}
