package com.zwei.web.controller.system;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.BatchIdRequest;
import com.zwei.common.core.domain.model.SysRoleQueryRequest;
import com.zwei.common.core.domain.model.SysRoleResponse;
import com.zwei.common.core.domain.model.SysRoleStatusRequest;
import com.zwei.common.core.domain.model.SysRoleUpsertRequest;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.framework.web.service.SysPermissionService;
import com.zwei.framework.web.service.TokenService;
import com.zwei.system.domain.SysUserRole;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色信息
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/roles")
public class SysRoleController extends BaseController
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

    /**
     * 分页查询角色列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/page")
    public AjaxResult list(SysRoleQueryRequest request)
    {
        startPage();
        SysRole role = request.toEntity();
        List<SysRole> list = roleService.selectRoleList(role);
        List<SysRoleResponse> rspList = list.stream()
                .map(SysRoleResponse::fromEntity)
                .collect(Collectors.toList());
        PageDomain pageDomain = TableSupport.buildPageRequest();
        long total = new PageInfo(list).getTotal();
        HashMap<String, Object> data = new HashMap<>();
        data.put("rows", rspList);
        data.put("total", total);
        data.put("pageNum", pageDomain.getPageNum());
        data.put("pageSize", pageDomain.getPageSize());
        return AjaxResult.success("成功", data);
    }

    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:role:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        util.exportExcel(response, list, "角色数据");
    }

    /**
     * 获取角色详情
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        roleService.checkRoleDataScope(id);
        SysRole role = roleService.selectRoleById(id);
        SysRoleResponse resp = SysRoleResponse.fromEntity(role);
        resp.setMenuIds(role.getMenuIds() != null ? java.util.Arrays.asList(role.getMenuIds()) : null);
        return AjaxResult.success("成功", resp);
    }

    /**
     * 新增角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysRoleUpsertRequest request)
    {
        if (StringUtils.isEmpty(request.getCode()) || StringUtils.isEmpty(request.getName()) || request.getSortOrder() == null)
        {
            return error("角色编码、角色名称、排序不能为空");
        }
        SysRole role = request.toEntity();
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setCreateBy(getUsername());
        roleService.insertRole(role);
        return AjaxResult.success("新增成功", new HashMap<String, Long>() {{
            put("id", role.getRoleId());
        }});
    }

    /**
     * 修改角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody SysRoleUpsertRequest request)
    {
        SysRole role = roleService.selectRoleById(id);
        if (role == null)
        {
            return error("角色不存在");
        }
        role.setRoleId(id);
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(id);
        request.applyTo(role, false);
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setUpdateBy(getUsername());
        roleService.updateRole(role);
        // 刷新所有持有该角色的在线用户权限
        tokenService.refreshPermissionByRoleId(role.getRoleId(), permissionService);
        return AjaxResult.success("修改成功");
    }

    /**
     * 删除角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        if (roleService.countUserRoleByRoleId(id) > 0) {
            return error("该角色下存在用户，无法删除");
        }
        return toAjax(roleService.deleteRoleByIds(new Long[]{id}));
    }

    /**
     * 批量删除角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult batchRemove(@Validated @RequestBody BatchIdRequest request) {
        Long[] ids = request.getIds().toArray(new Long[0]);
        for (Long id : ids) {
            if (roleService.countUserRoleByRoleId(id) > 0) {
                return error("角色ID " + id + " 下存在用户，无法删除");
            }
        }
        return toAjax(roleService.deleteRoleByIds(ids));
    }

    /**
     * 修改保存数据权限
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/dataScope")
    public AjaxResult dataScope(@PathVariable Long id, @Validated @RequestBody SysRoleUpsertRequest request)
    {
        SysRole role = new SysRole();
        role.setRoleId(id);
        role.setDataScope(request.getDataScope() == null ? null : String.valueOf(request.getDataScope()));
        role.setDeptIds(request.getDeptIds() == null ? null : request.getDeptIds().toArray(new Long[0]));
        role.setMenuIds(request.getMenuIds() == null ? null : request.getMenuIds().toArray(new Long[0]));
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(id);
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @Validated @RequestBody SysRoleStatusRequest request)
    {
        SysRole role = new SysRole();
        role.setRoleId(id);
        role.setStatus(request.getStatus());
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(id);
        role.setUpdateBy(getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 获取角色选择框列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return success(roleService.selectRoleAll());
    }

    /**
     * 查询已分配用户角色列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/allocatedList")
    public AjaxResult allocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectAllocatedList(user);
        return AjaxResult.success("成功", list);
    }

    /**
     * 查询未分配用户角色列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/unallocatedList")
    public AjaxResult unallocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUnallocatedList(user);
        return AjaxResult.success("成功", list);
    }

    /**
     * 取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole)
    {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 批量取消授权用户
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(Long roleId, Long[] userIds)
    {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    /**
     * 批量选择用户授权
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/selectAll")
    public AjaxResult selectAuthUserAll(Long roleId, Long[] userIds)
    {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    /**
     * 获取对应角色部门树列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/{id}/deptTree")
    public AjaxResult deptTree(@PathVariable("id") Long id)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(id));
        ajax.put("depts", deptService.selectDeptTreeList(new com.zwei.common.core.domain.entity.SysDept()));
        return ajax;
    }
}
