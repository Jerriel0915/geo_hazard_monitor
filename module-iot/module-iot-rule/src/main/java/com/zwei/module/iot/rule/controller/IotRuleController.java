package com.zwei.module.iot.rule.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.module.iot.rule.domain.IotRule;
import com.zwei.module.iot.rule.service.IIotRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IoT规则Controller
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-24
 */
@RestController
@RequestMapping("/iot/rule")
public class IotRuleController extends BaseController {
    private final IIotRuleService iotRuleService;

    @Autowired
    IotRuleController(IIotRuleService iotRuleService) {
        this.iotRuleService = iotRuleService;
    }

    /**
     * 查询规则列表
     */
    @PreAuthorize("@ss.hasPermi('iot:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list(IotRule iotRule) {
        startPage();
        List<IotRule> list = iotRuleService.selectIotRuleList(iotRule);
        return getDataTable(list);
    }

    /**
     * 获取规则详细信息
     */
    @PreAuthorize("@ss.hasPermi('iot:rule:query')")
    @GetMapping(value = "/{ruleId}")
    public AjaxResult getInfo(@PathVariable("ruleId") Long ruleId) {
        return AjaxResult.success(iotRuleService.selectIotRuleByRuleId(ruleId));
    }

    /**
     * 新增规则
     */
    @PreAuthorize("@ss.hasPermi('iot:rule:add')")
    @Log(title = "IoT规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody IotRule iotRule) {
        iotRule.setCreateBy(getUsername());
        return toAjax(iotRuleService.insertIotRule(iotRule));
    }

    /**
     * 修改规则
     */
    @PreAuthorize("@ss.hasPermi('iot:rule:edit')")
    @Log(title = "IoT规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody IotRule iotRule) {
        iotRule.setUpdateBy(getUsername());
        return toAjax(iotRuleService.updateIotRule(iotRule));
    }

    /**
     * 删除规则
     */
    @PreAuthorize("@ss.hasPermi('iot:rule:remove')")
    @Log(title = "IoT规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public AjaxResult remove(@PathVariable Long[] ruleIds) {
        return toAjax(iotRuleService.deleteIotRuleByRuleIds(ruleIds));
    }
}
