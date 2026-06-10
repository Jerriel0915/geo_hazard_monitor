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
    @PreAuthorize("@ss.hasPermi('iot:alarm-dispatch:list')")
    public TableDataInfo list(AlarmDispatchRule rule) {
        startPage();
        List<AlarmDispatchRule> list = dispatchService.selectList(rule);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-dispatch:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(dispatchService.selectById(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('iot:alarm-dispatch:create')")
    public AjaxResult create(@RequestBody DispatchRuleCreateRequest request) {
        if (!dispatchService.checkDispatchRuleUnique(request.getName(), request.getHazardPointId(), 0L)) {
            return error("新增失败，该隐患点下已存在同名分发规则");
        }
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
    @PreAuthorize("@ss.hasPermi('iot:alarm-dispatch:update')")
    public AjaxResult update(@PathVariable Long id, @RequestBody DispatchRuleCreateRequest request) {
        if (!dispatchService.checkDispatchRuleUnique(request.getName(), request.getHazardPointId(), id)) {
            return error("修改失败，该隐患点下已存在同名分发规则");
        }
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
    @PreAuthorize("@ss.hasPermi('iot:alarm-dispatch:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(dispatchService.delete(id));
    }
}
