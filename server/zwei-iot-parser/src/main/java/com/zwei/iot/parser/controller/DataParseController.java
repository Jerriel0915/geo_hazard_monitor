package com.zwei.iot.parser.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.parser.domain.DataParseLog;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.dto.DataParseStrategyDTO;
import com.zwei.iot.parser.dto.DataParseStrategyQueryDTO;
import com.zwei.iot.parser.dto.DataParseTestRequest;
import com.zwei.iot.parser.engine.GroovyScriptEngine;
import com.zwei.iot.parser.service.DataParseLogService;
import com.zwei.iot.parser.service.DataParseStrategyService;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/iot/parser/strategy")
public class DataParseController extends BaseController {

    @Resource
    private DataParseStrategyService strategyService;
    @Resource
    private DataParseLogService logService;
    @Resource
    private GroovyScriptEngine scriptEngine;

    @PreAuthorize("@ss.hasPermi('monitor:parser:list')")
    @GetMapping("/page")
    public TableDataInfo list(DataParseStrategyQueryDTO query) {
        startPage();
        List<DataParseStrategy> list = strategyService.listByPage(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:list')")
    @GetMapping("/{id}")
    public AjaxResult getDetail(@PathVariable Long id) {
        return AjaxResult.success(strategyService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @PostMapping
    public AjaxResult create(@RequestBody DataParseStrategyDTO dto) {
        return AjaxResult.success(strategyService.create(dto));
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody DataParseStrategyDTO dto) {
        strategyService.update(dto);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        strategyService.delete(id);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @PutMapping("/{id}/status")
    public AjaxResult toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        strategyService.toggleStatus(id, status);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id) {
        return AjaxResult.success(strategyService.copy(id));
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:test')")
    @PostMapping("/test")
    public AjaxResult testScript(@RequestBody DataParseTestRequest request) {
        Map<String, Object> result = scriptEngine.testScript(
                request.getScriptCode(), request.getTopic(), request.getTestData());
        return AjaxResult.success(result);
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:list')")
    @GetMapping("/{id}/logs")
    public TableDataInfo getLogs(@PathVariable Long id,
                                  @RequestParam(required = false) String logLevel,
                                  @RequestParam(required = false) String startTime,
                                  @RequestParam(required = false) String endTime) {
        startPage();
        List<DataParseLog> logs = logService.listByCondition(id, logLevel, startTime, endTime);
        return getDataTable(logs);
    }

    @PreAuthorize("@ss.hasPermi('monitor:parser:edit')")
    @DeleteMapping("/{id}/logs")
    public AjaxResult clearLogs(@PathVariable Long id) {
        logService.clearByStrategyId(id);
        return AjaxResult.success();
    }
}
