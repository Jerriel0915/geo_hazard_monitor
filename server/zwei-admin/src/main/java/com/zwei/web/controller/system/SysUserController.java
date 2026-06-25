package com.zwei.web.controller.system;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.entity.SysRole;
import com.zwei.common.core.domain.entity.SysUser;
import com.zwei.common.core.domain.model.BatchIdRequest;
import com.zwei.common.core.domain.model.SysUserPasswordRequest;
import com.zwei.common.core.domain.model.SysUserQueryRequest;
import com.zwei.common.core.domain.model.SysUserResponse;
import com.zwei.common.core.domain.model.SysUserStatusRequest;
import com.zwei.common.core.domain.model.SysUserUpsertRequest;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.system.service.ISysDeptService;
import com.zwei.system.service.ISysPostService;
import com.zwei.system.service.ISysRoleService;
import com.zwei.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/users")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    /**
     * 分页查询用户列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/page")
    public AjaxResult list(SysUserQueryRequest request)
    {
        startPage();
        SysUser user = request.toEntity();
        List<SysUser> list = userService.selectUserList(user);
        List<SysUserResponse> rspList = list.stream()
                .map(SysUserResponse::fromEntity)
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

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 获取用户详情
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        userService.checkUserDataScope(id);
        SysUser sysUser = userService.selectUserById(id);
        SysUserResponse resp = SysUserResponse.fromEntity(sysUser);
        resp.setPostIds(postService.selectPostListByUserId(id));
        resp.setRoleIds(sysUser.getRoles() == null ? null
                : sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        return AjaxResult.success("成功", resp);
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUserUpsertRequest request)
    {
        if (StringUtils.isEmpty(request.getUsername()) || StringUtils.isEmpty(request.getPassword()) || StringUtils.isEmpty(request.getRealName()))
        {
            return error("用户名、密码、真实姓名不能为空");
        }
        SysUser user = request.toEntity();
        deptService.checkDeptDataScope(user.getDeptId());
        if (user.getRoleIds() != null)
        {
            roleService.checkRoleDataScope(user.getRoleIds());
        }
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        userService.insertUser(user);
        return AjaxResult.success("新增成功", new HashMap<String, Long>() {{
            put("id", user.getUserId());
        }});
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody SysUserUpsertRequest request)
    {
        SysUser user = userService.selectUserById(id);
        if (user == null)
        {
            return error("用户不存在");
        }
        user.setUserId(id);
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(id);
        request.applyTo(user, false);
        deptService.checkDeptDataScope(user.getDeptId());
        if (user.getRoleIds() != null)
        {
            roleService.checkRoleDataScope(user.getRoleIds());
        }
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        if (id.equals(getUserId())) {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(new Long[]{id}));
    }

    /**
     * 批量删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult batchRemove(@Validated @RequestBody BatchIdRequest request)
    {
        Long[] ids = request.getIds().toArray(new Long[0]);
        if (ArrayUtils.contains(ids, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(ids));
    }

    /**
     * 修改密码
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/password")
    public AjaxResult changePassword(@PathVariable Long id, @Validated @RequestBody SysUserPasswordRequest request)
    {
        SysUser user = userService.selectUserById(id);
        if (user == null)
        {
            return error("用户不存在");
        }
        user.setUserId(id);
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(id);
        user.setOldPassword(request.getOldPassword());
        user.setNewPassword(request.getNewPassword());
        if (!SecurityUtils.matchesPassword(user.getOldPassword(), userService.selectUserById(id).getPassword())) {
            return error("旧密码不正确");
        }
        user.setUserId(id);
        user.setPassword(SecurityUtils.encryptPassword(user.getNewPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@Validated @RequestBody SysUserStatusRequest request)
    {
        Long userId = request.getUserId();
        String status = request.getStatus();
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setStatus(status);
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(userId);
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 获取角色授权信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", SysUserResponse.fromEntity(user));
        List<SysRole> filterRoles = SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList());
        ajax.put("roles", filterRoles);
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }
}
