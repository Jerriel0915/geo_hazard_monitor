package com.zwei.iot.timeseries.compute.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.timeseries.compute.ComputedAttribute;
import com.zwei.iot.timeseries.compute.ComputedAttributeRegistry;
import com.zwei.iot.timeseries.compute.ComputedScriptAssembler;
import com.zwei.iot.timeseries.compute.dto.CalcScriptTestRequest;
import com.zwei.iot.timeseries.compute.dto.CalcScriptTestResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 计算脚本在线测试端点。
 *
 * <p>路径前缀: /api/v1/monitor-contents
 * (沿用 monitor-contents 资源语义, 但 controller 在 timeseries 模块以避免依赖反向)
 */
@RestController
@RequestMapping("api/v1/monitor-contents")
public class ComputedAttributeTestController extends BaseController {

    private final ComputedAttributeRegistry registry;
    private final ComputedScriptAssembler assembler;
    private final GroovyScriptEngine scriptEngine;

    @Autowired
    public ComputedAttributeTestController(ComputedAttributeRegistry registry,
                                           ComputedScriptAssembler assembler,
                                           GroovyScriptEngine scriptEngine) {
        this.registry = registry;
        this.assembler = assembler;
        this.scriptEngine = scriptEngine;
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorContent:test')")
    @PostMapping("/test-script")
    public AjaxResult testScript(@Valid @RequestBody CalcScriptTestRequest request) {
        // 1. 加载该监测类型下其他计算属性(作为上下文)
        List<ComputedAttribute> existing = registry.getByMonitorTypeId(request.getMonitorTypeId());

        // 2. 用请求中的脚本替换目标 attrCode(若不存在则追加)
        List<ComputedAttribute> merged = new ArrayList<>();
        boolean replaced = false;
        for (ComputedAttribute a : existing) {
            if (a.code().equals(request.getAttrCode())) {
                merged.add(new ComputedAttribute(
                        -1L, request.getMonitorTypeId(), a.code(), a.name(),
                        a.unit(), request.getCalcScript(), a.sortOrder()));
                replaced = true;
            } else {
                merged.add(a);
            }
        }
        if (!replaced) {
            merged.add(new ComputedAttribute(
                    -1L, request.getMonitorTypeId(), request.getAttrCode(),
                    request.getAttrCode(), "", request.getCalcScript(),
                    merged.size() + 1));
        }

        // 3. 拼装 + 执行
        String script = assembler.assemble(merged);
        Map<String, Object> curData = request.getCurData() == null ? Map.of() : request.getCurData();
        Map<String, Object> prevData = request.getPrevData();

        long start = System.currentTimeMillis();
        Map<String, Object> result = scriptEngine.executeComputed(script, curData, prevData);
        long elapsed = System.currentTimeMillis() - start;

        // 4. 返回(只取目标 attrCode 的结果)
        Object targetValue = result.get(request.getAttrCode());
        if (targetValue == null) {
            return AjaxResult.success("脚本执行失败或返回 null",
                    CalcScriptTestResult.fail("属性 '" + request.getAttrCode() + "' 未返回有效结果"));
        }
        return AjaxResult.success("成功",
                CalcScriptTestResult.ok(Map.of(request.getAttrCode(), targetValue), elapsed));
    }
}
