package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.domain.TerraTool;
import com.zwei.terra.agent.service.ITerraToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Terra 工具管理 Controller
 */
@RestController
@RequestMapping("/api/v1/terra/tools")
public class TerraToolController extends BaseController {

    @Autowired
    private ITerraToolService toolService;

    /**
     * 获取工具列表
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult list() {
        return success(toolService.selectList());
    }

    /**
     * 新增工具
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult add(@RequestBody TerraTool tool) {
        TerraTool created = toolService.create(tool, getUsername());
        return success(created);
    }

    /**
     * 修改工具
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult edit(@RequestBody TerraTool tool) {
        toolService.update(tool, getUsername());
        return success();
    }

    /**
     * 删除工具
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult remove(@PathVariable Long id) {
        toolService.delete(id);
        return success();
    }

    /**
     * 启用/停用工具
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult toggle(@PathVariable Long id) {
        toolService.toggle(id, getUsername());
        return success();
    }
}
