package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.service.ITerraSkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Terra 技能管理 Controller
 */
@RestController
@RequestMapping("/api/v1/terra/skills")
public class TerraSkillController extends BaseController {

    @Autowired
    private ITerraSkillService skillService;

    /**
     * 获取技能列表
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult list() {
        return success(skillService.selectList());
    }

    /**
     * 获取技能详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(skillService.selectById(id));
    }

    /**
     * 卸载技能
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult remove(@PathVariable Long id) {
        skillService.uninstall(id, getUsername());
        return success();
    }

    /**
     * 启用/停用技能
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult toggle(@PathVariable Long id) {
        skillService.toggle(id, getUsername());
        return success();
    }
}
