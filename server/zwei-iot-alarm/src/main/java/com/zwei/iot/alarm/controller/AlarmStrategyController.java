package com.zwei.iot.alarm.controller;

import com.zwei.common.annotation.RepeatSubmit;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.dto.StrategyCreateRequest;
import com.zwei.iot.alarm.service.IAlarmStrategyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 综合告警策略管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/strategies")
public class AlarmStrategyController extends BaseController {

    private final IAlarmStrategyService strategyService;

    public AlarmStrategyController(IAlarmStrategyService strategyService) {
        this.strategyService = strategyService;
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
                .escalationEnabled(request.getEscalationEnabled() != null ? request.getEscalationEnabled() : 0)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .createBy(getUsername())
                .build();
        return toAjax(strategyService.insert(strategy, request.getHazardPointIds()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-strategy:update')")
    public AjaxResult update(@PathVariable Long id, @Validated @RequestBody StrategyCreateRequest request) {
        if (!strategyService.checkStrategyNameUnique(request.getName(), id)) {
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
                .escalationEnabled(request.getEscalationEnabled())
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
        return success(strategyService.getHazardPointIds(id));
    }
}
