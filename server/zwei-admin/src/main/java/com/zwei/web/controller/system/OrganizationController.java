package com.zwei.web.controller.system;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.constant.UserConstants;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.domain.entity.SysDept;
import com.zwei.common.core.domain.model.SysOrganizationQueryRequest;
import com.zwei.common.core.domain.model.SysOrganizationResponse;
import com.zwei.common.core.domain.model.SysOrganizationUpsertRequest;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.StringUtils;
import com.zwei.system.service.ISysDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标准化组织管理接口
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController extends BaseController
{
    @Autowired
    private ISysDeptService deptService;

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/tree")
    public AjaxResult tree(SysOrganizationQueryRequest request)
    {
        SysDept query = request.toEntity();
        List<SysDept> depts = deptService.selectDeptList(query);
        List<SysOrganizationResponse> data = deptService.buildDeptTree(depts).stream()
                .map(SysOrganizationResponse::fromEntity)
                .collect(Collectors.toList());
        return AjaxResult.success("成功", data);
    }

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/page")
    public AjaxResult page(SysOrganizationQueryRequest request)
    {
        startPage();
        SysDept query = request.toEntity();
        List<SysDept> depts = deptService.selectDeptList(query);
        List<SysOrganizationResponse> rows = depts.stream().map(SysOrganizationResponse::fromEntity).collect(Collectors.toList());
        PageDomain pageDomain = TableSupport.buildPageRequest();
        HashMap<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", new PageInfo(depts).getTotal());
        data.put("pageNum", pageDomain.getPageNum());
        data.put("pageSize", pageDomain.getPageSize());
        return AjaxResult.success("成功", data);
    }

    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        deptService.checkDeptDataScope(id);
        return AjaxResult.success("成功", SysOrganizationResponse.fromEntity(deptService.selectDeptById(id)));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @Log(title = "组织管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysOrganizationUpsertRequest request)
    {
        if (StringUtils.isEmpty(request.getCode()) || StringUtils.isEmpty(request.getName()) || request.getSortOrder() == null)
        {
            return error("组织编码、组织名称、排序不能为空");
        }
        SysDept dept = request.toEntity();
        if (!deptService.checkDeptCodeUnique(dept))
        {
            return error("新增组织'" + dept.getDeptName() + "'失败，组织编码已存在");
        }
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增组织'" + dept.getDeptName() + "'失败，同级组织名称已存在");
        }
        dept.setCreateBy(getUsername());
        deptService.insertDept(dept);
        return AjaxResult.success("新增成功", new HashMap<String, Long>() {{
            put("id", dept.getDeptId());
        }});
    }

    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "组织管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody SysOrganizationUpsertRequest request)
    {
        SysDept dept = request.toEntity();
        dept.setDeptId(id);
        deptService.checkDeptDataScope(id);
        if (!deptService.checkDeptCodeUnique(dept))
        {
            return error("修改组织'" + dept.getDeptName() + "'失败，组织编码已存在");
        }
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改组织'" + dept.getDeptName() + "'失败，同级组织名称已存在");
        }
        if (dept.getParentId().equals(id))
        {
            return error("修改组织'" + dept.getDeptName() + "'失败，上级组织不能是自己");
        }
        if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(id) > 0)
        {
            return error("该组织包含未停用的子组织！");
        }
        dept.setUpdateBy(getUsername());
        deptService.updateDept(dept);
        return AjaxResult.success("修改成功");
    }

    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "组织管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        if (deptService.hasChildByDeptId(id))
        {
            return error("存在下级组织,不允许删除");
        }
        if (deptService.checkDeptExistUser(id))
        {
            return error("组织存在用户,不允许删除");
        }
        deptService.checkDeptDataScope(id);
        return toAjax(deptService.deleteDeptById(id));
    }
}
