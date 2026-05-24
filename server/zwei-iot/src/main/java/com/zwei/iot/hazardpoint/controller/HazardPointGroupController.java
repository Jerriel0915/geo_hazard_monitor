package com.zwei.iot.hazardpoint.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.hazardpoint.domain.dto.HazardPointGroupCreateRequest;
import com.zwei.iot.hazardpoint.domain.dto.HazardPointGroupUpdateRequest;
import com.zwei.iot.hazardpoint.service.IHazardPointGroupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

/**
 * 隐患点分组管理
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/hazard-point-groups")
public class HazardPointGroupController extends BaseController
{
    private final IHazardPointGroupService hazardPointGroupService;

    public HazardPointGroupController(IHazardPointGroupService hazardPointGroupService)
    {
        this.hazardPointGroupService = hazardPointGroupService;
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:list')")
    @GetMapping
    public AjaxResult list(HazardPointGroup group)
    {
        List<HazardPointGroup> list = hazardPointGroupService.selectHazardPointGroupList(group);
        return AjaxResult.success("成功", list);
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        HazardPointGroup group = hazardPointGroupService.selectHazardPointGroupById(id);
        return group == null ? AjaxResult.error("分组不存在") : AjaxResult.success("成功", group);
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:add')")
    @Log(title = "隐患点分组", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HazardPointGroupCreateRequest request)
    {
        HazardPointGroup group = buildGroup(request);
        if (!hazardPointGroupService.checkGroupCodeUnique(group))
        {
            return error("新增分组失败，分组编码已存在");
        }
        group.setCreateBy(getUsername());
        int rows = hazardPointGroupService.insertHazardPointGroup(group);
        return rows > 0
                ? AjaxResult.success("新增成功", Map.of("id", group.getId()))
                : AjaxResult.error("新增失败");
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:edit')")
    @Log(title = "隐患点分组", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody HazardPointGroupUpdateRequest request)
    {
        HazardPointGroup group = buildGroup(request);
        group.setId(id);
        if (!hazardPointGroupService.checkGroupCodeUnique(group))
        {
            return error("修改分组失败，分组编码已存在");
        }
        group.setUpdateBy(getUsername());
        int rows = hazardPointGroupService.updateHazardPointGroup(group);
        return rows > 0
                ? AjaxResult.success("修改成功").put(AjaxResult.DATA_TAG, null)
                : AjaxResult.error("分组不存在");
    }

    @PreAuthorize("@ss.hasPermi('basic:hazardPointGroup:remove')")
    @Log(title = "隐患点分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        hazardPointGroupService.deleteHazardPointGroupById(id);
        return AjaxResult.success("删除成功").put(AjaxResult.DATA_TAG, null);
    }

    private HazardPointGroup buildGroup(HazardPointGroupCreateRequest request)
    {
        HazardPointGroup group = new HazardPointGroup();
        group.setCode(trimToNull(request.getCode()));
        group.setName(trimToNull(request.getName()));
        group.setDescription(trimToNull(request.getDescription()));
        group.setSortOrder(request.getSortOrder());
        group.setStatus(request.getStatus());
        return group;
    }

    private HazardPointGroup buildGroup(HazardPointGroupUpdateRequest request)
    {
        HazardPointGroup group = new HazardPointGroup();
        group.setCode(trimToNull(request.getCode()));
        group.setName(trimToNull(request.getName()));
        group.setDescription(trimToNull(request.getDescription()));
        group.setSortOrder(request.getSortOrder());
        group.setStatus(request.getStatus());
        return group;
    }

    private String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
