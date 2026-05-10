package com.zwei.iot.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.domain.HazardPointGroup;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.service.IHazardPointGroupService;

/**
 * 隐患点分组管理
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/hazard-point-groups")
public class HazardPointGroupController extends BaseController
{
    @Autowired
    private IHazardPointGroupService hazardPointGroupService;

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:list')")
    @GetMapping
    public AjaxResult list(HazardPointGroup group)
    {
        List<HazardPointGroup> list = hazardPointGroupService.selectHazardPointGroupList(group);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(hazardPointGroupService.selectHazardPointGroupById(id));
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:add')")
    @Log(title = "隐患点分组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HazardPointGroup group)
    {
        if (!hazardPointGroupService.checkGroupCodeUnique(group))
        {
            return error("新增分组'" + group.getName() + "'失败，分组编码已存在");
        }
        group.setCreateBy(getUsername());
        int rows = hazardPointGroupService.insertHazardPointGroup(group);
        return rows > 0 ? success(group.getId()) : error("新增失败");
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:edit')")
    @Log(title = "隐患点分组", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody HazardPointGroup group)
    {
        group.setId(id);
        if (!hazardPointGroupService.checkGroupCodeUnique(group))
        {
            return error("修改分组'" + group.getName() + "'失败，分组编码已存在");
        }
        group.setUpdateBy(getUsername());
        int rows = hazardPointGroupService.updateHazardPointGroup(group);
        return rows > 0 ? success() : error("修改失败");
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:remove')")
    @Log(title = "隐患点分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        HazardPointGroup group = hazardPointGroupService.selectHazardPointGroupById(id);
        if (group != null && group.getCount() != null && group.getCount() > 0)
        {
            return warn("该分组下存在隐患点，不允许删除");
        }
        int rows = hazardPointGroupService.deleteHazardPointGroupById(id);
        return rows > 0 ? success() : error("删除失败");
    }
}
