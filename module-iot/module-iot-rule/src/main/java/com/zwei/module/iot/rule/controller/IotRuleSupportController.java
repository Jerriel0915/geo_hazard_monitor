package com.zwei.module.iot.rule.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.module.iot.rule.domain.RuleTestRequest;
import com.zwei.module.iot.rule.domain.RuleValidateRequest;
import com.zwei.module.iot.rule.service.IRuleSupportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * IoT规则测试 Controller
 * 接收前端回传的 Aviator 表达式，检测是否合法并可通过编译
 *
 * @Author: Jerriel
 * @CreateTime: 2026-03-05
 */
@Slf4j
@RestController
@RequestMapping("/iot/rule")
public class IotRuleSupportController {
    private final IRuleSupportService ruleSupportService;

    @Autowired
    public IotRuleSupportController(IRuleSupportService ruleSupportService) {
        this.ruleSupportService = ruleSupportService;
    }

    /**
     * 语法校验
     *
     * @param req 封装后的表达式
     * @return
     */
    @PostMapping("/validate")
    @PreAuthorize("@ss.hasPermi('iot:rule:validate')")
    @Log(title = "规则表达式校验", businessType = BusinessType.OTHER)
    public AjaxResult validate(@Validated @RequestBody RuleValidateRequest req) {
        boolean ok = ruleSupportService.validate(req.getRuleExpression());
        return AjaxResult.success().put("ok", ok);
    }

    /**
     * 测试
     *
     * @param req 封装后的表达式（含测试参数）
     * @return
     */
    @PostMapping("/test")
    @PreAuthorize("@ss.hasPermi('iot:rule:test')")
    @Log(title = "规则测试", businessType = BusinessType.OTHER)
    public AjaxResult test(@Validated @RequestBody RuleTestRequest req) {
        boolean match = ruleSupportService.test(req.getRuleExpression(), req.getContext());
        return AjaxResult.success().put("match", match);
    }
}
