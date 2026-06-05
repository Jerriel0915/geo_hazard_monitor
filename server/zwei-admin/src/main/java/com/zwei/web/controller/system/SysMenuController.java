package com.zwei.web.controller.system;

import com.zwei.common.annotation.Log;
import com.zwei.common.constant.UserConstants;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysMenu;
import com.zwei.common.core.domain.model.SysMenuUpsertRequest;
import com.zwei.common.core.domain.model.SysMenuResponse;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.StringUtils;
import com.zwei.system.service.ISysMenuService;
import com.zwei.system.service.impl.PermissionCoverageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜单信息
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/menus")
public class SysMenuController extends BaseController
{
    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private PermissionCoverageService permissionCoverageService;

    /**
     * 获取菜单列表（树形）
     */
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/tree")
    public AjaxResult tree(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return AjaxResult.success("成功", SysMenuResponse.buildTree(menus));
    }

    /**
     * 获取当前用户菜单
     */
    @GetMapping("/current")
    public AjaxResult current() {
        List<SysMenu> menus = menuService.selectMenuList(getUserId());
        return AjaxResult.success("成功", SysMenuResponse.buildTree(menus));
    }

    /**
     * 获取菜单详情
     */
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        SysMenu menu = menuService.selectMenuById(id);
        return AjaxResult.success("成功", SysMenuResponse.fromEntity(menu));
    }

    /**
     * 获取菜单下拉树列表
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId)
    {
        List<SysMenu> menus = menuService.selectMenuList(getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    /**
     * 新增菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenuUpsertRequest request)
    {
        SysMenu menu = request.toEntity();
        if (StringUtils.isEmpty(menu.getMenuName()) || menu.getParentId() == null
                || StringUtils.isEmpty(menu.getMenuType()) || menu.getOrderNum() == null)
        {
            return error("父级菜单、菜单名称、菜单类型、排序不能为空");
        }
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setCreateBy(getUsername());
        menuService.insertMenu(menu);
        return AjaxResult.success("新增成功", new HashMap<String, Long>() {{
            put("id", menu.getMenuId());
        }});
    }

    /**
     * 修改菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody SysMenuUpsertRequest request)
    {
        SysMenu menu = menuService.selectMenuById(id);
        if (menu == null)
        {
            return error("菜单不存在");
        }
        menu.setMenuId(id);
        request.applyTo(menu, false);
        if (StringUtils.isEmpty(menu.getMenuName()) || menu.getParentId() == null
                || StringUtils.isEmpty(menu.getMenuType()) || menu.getOrderNum() == null)
        {
            return error("父级菜单、菜单名称、菜单类型、排序不能为空");
        }
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (menu.getParentId() != null && menu.getMenuId().equals(menu.getParentId()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setUpdateBy(getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 保存菜单排序
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "保存菜单排序", businessType = BusinessType.UPDATE)
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody Map<String, String> params)
    {
        String[] menuIds = params.get("menuIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        menuService.updateMenuSort(menuIds, orderNums);
        return success();
    }

    /**
     * 删除菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable("id") Long id)
    {
        if (menuService.hasChildByMenuId(id))
        {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(id))
        {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(id));
    }

    /**
     * 批量删除菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult batchRemove(@RequestBody Long[] ids) {
        for (Long id : ids) {
            if (menuService.hasChildByMenuId(id)) {
                return error("菜单ID " + id + " 存在子菜单，无法删除");
            }
            if (menuService.checkMenuExistRole(id)) {
                return error("菜单ID " + id + " 已分配角色，无法删除");
            }
        }
        int result = 0;
        for (Long id : ids) {
            result += menuService.deleteMenuById(id);
        }
        return result > 0 ? success() : error();
    }

    /**
     * 权限覆盖报告：代码 @PreAuthorize vs 数据库 sys_menu.perms
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/permission-coverage")
    public AjaxResult permissionCoverage() {
        return AjaxResult.success("成功", permissionCoverageService.getCoverageReport());
    }

    /**
     * 批量注册缺失权限到菜单表
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PostMapping("/batch-register")
    public AjaxResult batchRegister(@RequestBody List<String> perms) {
        int count = permissionCoverageService.batchRegister(perms);
        return AjaxResult.success("成功注册 " + count + " 条权限");
    }
}
