package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.domain.TerraModelConfig;
import com.zwei.terra.agent.service.ITerraModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Terra 模型配置 Controller
 */
@RestController
@RequestMapping("/api/v1/terra/model-configs")
public class TerraModelConfigController extends BaseController {

    @Autowired
    private ITerraModelConfigService modelConfigService;

    /**
     * 获取模型配置列表
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult list() {
        return success(modelConfigService.selectList());
    }

    /**
     * 获取模型配置详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(modelConfigService.selectById(id));
    }

    /**
     * 新增模型配置
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult add(@RequestBody TerraModelConfig config) {
        TerraModelConfig created = modelConfigService.create(config, getUsername());
        return success(created);
    }

    /**
     * 修改模型配置
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult edit(@RequestBody TerraModelConfig config) {
        modelConfigService.update(config, getUsername());
        return success();
    }

    /**
     * 删除模型配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult remove(@PathVariable Long id) {
        modelConfigService.delete(id);
        return success();
    }

    /**
     * 激活指定模型配置
     */
    @PutMapping("/{id}/activate")
    @PreAuthorize("@ss.hasPermi('terra:settings')")
    public AjaxResult activate(@PathVariable Long id) {
        modelConfigService.activate(id, getUsername());
        return success();
    }
}
