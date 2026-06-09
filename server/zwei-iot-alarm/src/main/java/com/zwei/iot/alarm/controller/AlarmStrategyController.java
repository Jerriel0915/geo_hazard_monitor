package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmStrategy;
import com.zwei.iot.alarm.domain.dto.StrategyCreateRequest;
import com.zwei.iot.alarm.service.IAlarmStrategyService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('iot:alarm-strategy:list')")
    public TableDataInfo list(AlarmStrategy strategy) {
        startPage();
        List<AlarmStrategy> list = strategyService.selectList(strategy);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:alarm-strategy:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(strategyService.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('iot:alarm-strategy:create')")
    public AjaxResult create(@RequestBody StrategyCreateRequest request) {
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
    @PreAuthorize("hasAuthority('iot:alarm-strategy:update')")
    public AjaxResult update(@PathVariable Long id, @RequestBody StrategyCreateRequest request) {
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
    @PreAuthorize("hasAuthority('iot:alarm-strategy:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(strategyService.delete(id));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('iot:alarm-strategy:toggle')")
    public AjaxResult toggle(@PathVariable Long id, @RequestParam Integer isEnabled) {
        return toAjax(strategyService.toggle(id, isEnabled));
    }

    @GetMapping("/{id}/scope")
    @PreAuthorize("hasAuthority('iot:alarm-strategy:list')")
    public AjaxResult getScope(@PathVariable Long id) {
        return success(strategyService.getHazardPointIds(id));
    }
}
