package com.zwei.iot.alarm.controller;

import com.zwei.common.annotation.RepeatSubmit;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.StrategyExecutionLog;
import com.zwei.iot.alarm.domain.dto.StrategyCreateRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunRequest;
import com.zwei.iot.alarm.domain.dto.StrategyTestRunResult;
import com.zwei.iot.alarm.mapper.StrategyExecutionLogMapper;
import com.zwei.iot.alarm.service.IAlarmStrategyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合告警策略管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/strategies")
public class AlarmStrategyController extends BaseController {

    private final IAlarmStrategyService strategyService;
    private final StrategyExecutionLogMapper executionLogMapper;

    public AlarmStrategyController(IAlarmStrategyService strategyService,
                                   StrategyExecutionLogMapper executionLogMapper) {
        this.strategyService = strategyService;
        this.executionLogMapper = executionLogMapper;
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
    public TableDataInfo list(AlarmStrategy strategy) {
        startPage();
        List<AlarmStrategy> list = strategyService.selectList(strategy);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(strategyService.selectById(id));
    }

    @RepeatSubmit
    @PostMapping
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:create')")
    public AjaxResult create(@Validated @RequestBody StrategyCreateRequest request) {
        if (!strategyService.checkStrategyNameUnique(request.getName(), 0L)) {
            return error("新增失败，策略名称已存在");
        }
        AlarmStrategy strategy = AlarmStrategy.builder()
                .name(request.getName())
                .description(request.getDescription())
                .monitorTypeId(request.getMonitorTypeId())
                .triggerMode(request.getTriggerMode())
                .cronExpression(request.getCronExpression())
                .scriptType(request.getScriptType() != null ? request.getScriptType() : "GROOVY")
                .scriptContent(request.getScriptContent())
                .defaultAlarmLevel(request.getDefaultAlarmLevel())
                .silenceMinutes(request.getSilenceMinutes() != null ? request.getSilenceMinutes() : 0)
                .sustainSeconds(request.getSustainSeconds() != null ? request.getSustainSeconds() : 0)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .createBy(getUsername())
                .build();
        return toAjax(strategyService.insert(strategy, request.getHazardPointIds()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:update')")
    public AjaxResult update(@PathVariable Long id, @RequestBody StrategyCreateRequest request) {
        if (request.getName() != null && !request.getName().isBlank()
                && !strategyService.checkStrategyNameUnique(request.getName(), id)) {
            return error("修改失败，策略名称已存在");
        }
        AlarmStrategy strategy = AlarmStrategy.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .monitorTypeId(request.getMonitorTypeId())
                .triggerMode(request.getTriggerMode())
                .cronExpression(request.getCronExpression())
                .scriptType(request.getScriptType())
                .scriptContent(request.getScriptContent())
                .defaultAlarmLevel(request.getDefaultAlarmLevel())
                .silenceMinutes(request.getSilenceMinutes())
                .sustainSeconds(request.getSustainSeconds() != null ? request.getSustainSeconds() : 0)
                .isEnabled(request.getIsEnabled())
                .updateBy(getUsername())
                .build();
        return toAjax(strategyService.update(strategy, request.getHazardPointIds()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(strategyService.delete(id));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:toggle')")
    public AjaxResult toggle(@PathVariable Long id, @RequestParam Integer isEnabled) {
        return toAjax(strategyService.toggle(id, isEnabled));
    }

    @GetMapping("/{id}/scope")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
    public AjaxResult getScope(@PathVariable Long id) {
        return success(strategyService.getScopeValues(id));
    }

    @PutMapping("/{id}/scope")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:update')")
    public AjaxResult updateScope(@PathVariable Long id, @RequestBody Map<String, String[]> body) {
        String[] hazardPointIds = body.get("hazardPointIds");
        return toAjax(strategyService.updateScope(id, hazardPointIds));
    }

    @PostMapping("/{id}/test-run")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
    public AjaxResult testRun(@PathVariable Long id, @RequestBody(required = false) StrategyTestRunRequest request) {
        return success(strategyService.testRun(id, request));
    }

    /**
     * 查询策略执行日志。
     */
    @GetMapping("/{id}/execution-logs")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:list')")
    public AjaxResult executionLogs(@PathVariable Long id,
                                     @RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "20") int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        List<StrategyExecutionLog> rows = executionLogMapper.selectByStrategyId(id, offset, pageSize);
        long total = executionLogMapper.countByStrategyId(id);
        Map<String, Object> data = new HashMap<>();
        data.put("rows", rows);
        data.put("total", total);
        return success(data);
    }
}
