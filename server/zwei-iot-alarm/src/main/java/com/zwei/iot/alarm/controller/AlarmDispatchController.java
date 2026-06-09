package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.domain.dto.DispatchRuleCreateRequest;
import com.zwei.iot.alarm.service.IAlarmDispatchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警分发规则管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/dispatch")
public class AlarmDispatchController extends BaseController {

    private final IAlarmDispatchService dispatchService;

    public AlarmDispatchController(IAlarmDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('iot:alarm-dispatch:list')")
    public TableDataInfo list(AlarmDispatchRule rule) {
        startPage();
        List<AlarmDispatchRule> list = dispatchService.selectList(rule);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:alarm-dispatch:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(dispatchService.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('iot:alarm-dispatch:create')")
    public AjaxResult create(@RequestBody DispatchRuleCreateRequest request) {
        AlarmDispatchRule rule = AlarmDispatchRule.builder()
                .name(request.getName())
                .hazardPointId(request.getHazardPointId())
                .alarmLevels(request.getAlarmLevels())
                .alarmTypes(request.getAlarmTypes())
                .recipientsJson(request.getRecipientsJson())
                .channels(request.getChannels() != null ? request.getChannels() : "SYSTEM")
                .timeWindow(request.getTimeWindow())
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .createBy(getUsername())
                .build();
        return toAjax(dispatchService.insert(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:alarm-dispatch:update')")
    public AjaxResult update(@PathVariable Long id, @RequestBody DispatchRuleCreateRequest request) {
        AlarmDispatchRule rule = AlarmDispatchRule.builder()
                .id(id)
                .name(request.getName())
                .hazardPointId(request.getHazardPointId())
                .alarmLevels(request.getAlarmLevels())
                .alarmTypes(request.getAlarmTypes())
                .recipientsJson(request.getRecipientsJson())
                .channels(request.getChannels())
                .timeWindow(request.getTimeWindow())
                .isEnabled(request.getIsEnabled())
                .updateBy(getUsername())
                .build();
        return toAjax(dispatchService.update(rule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('iot:alarm-dispatch:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(dispatchService.delete(id));
    }
}
