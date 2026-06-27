package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.domain.TerraPersonality;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Terra 人格配置 Controller
 */
@RestController
@RequestMapping("/api/v1/terra/personality")
public class TerraPersonalityController extends BaseController {

    @Autowired
    private ITerraPersonalityService personalityService;

    /**
     * 获取人格配置列表
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult list() {
        return success(personalityService.selectList());
    }

    /**
     * 更新人格配置
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult update(@RequestBody TerraPersonality personality) {
        personalityService.updateRole(personality, getUsername());
        return success();
    }

    /**
     * 切换启用/停用状态
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult toggle(@PathVariable Long id) {
        personalityService.toggleActive(id, getUsername());
        return success();
    }
}
