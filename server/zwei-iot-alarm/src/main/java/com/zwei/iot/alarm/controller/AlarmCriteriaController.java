package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmCriteria;
import com.zwei.iot.alarm.domain.AlarmCriteriaLog;
import com.zwei.iot.alarm.domain.dto.CriteriaCreateRequest;
import com.zwei.iot.alarm.service.IAlarmCriteriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警判据管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/criteria")
public class AlarmCriteriaController extends BaseController {

    private final IAlarmCriteriaService criteriaService;

    public AlarmCriteriaController(IAlarmCriteriaService criteriaService) {
        this.criteriaService = criteriaService;
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:list')")
    public TableDataInfo list(AlarmCriteria criteria) {
        startPage();
        List<AlarmCriteria> list = criteriaService.selectList(criteria);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(criteriaService.selectById(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:create')")
    public AjaxResult create(@RequestBody CriteriaCreateRequest request) {
        AlarmCriteria criteria = AlarmCriteria.builder()
                .name(request.getName())
                .monitorTypeId(request.getMonitorTypeId())
                .monitorTypeName(request.getMonitorTypeName())
                .monitorContentId(request.getMonitorContentId())
                .monitorContentCode(request.getMonitorContentCode())
                .hazardPointId(request.getHazardPointId())
                .levelConfig(request.getLevelConfig())
                .persistCount(request.getPersistCount() != null ? request.getPersistCount() : 1)
                .silencePeriod(request.getSilencePeriod() != null ? request.getSilencePeriod() : 0)
                .isEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : 1)
                .createBy(getUsername())
                .build();
        return toAjax(criteriaService.insert(criteria));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:update')")
    public AjaxResult update(@PathVariable Long id, @RequestBody CriteriaCreateRequest request) {
        AlarmCriteria criteria = AlarmCriteria.builder()
                .id(id)
                .name(request.getName())
                .monitorTypeId(request.getMonitorTypeId())
                .monitorTypeName(request.getMonitorTypeName())
                .monitorContentId(request.getMonitorContentId())
                .monitorContentCode(request.getMonitorContentCode())
                .hazardPointId(request.getHazardPointId())
                .levelConfig(request.getLevelConfig())
                .persistCount(request.getPersistCount())
                .silencePeriod(request.getSilencePeriod())
                .updateBy(getUsername())
                .build();
        return toAjax(criteriaService.update(criteria));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(criteriaService.delete(id));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:toggle')")
    public AjaxResult toggle(@PathVariable Long id, @RequestParam Integer isEnabled) {
        return toAjax(criteriaService.toggle(id, isEnabled));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@ss.hasPermi('iot:alarm-criteria:list')")
    public AjaxResult getLogs(@PathVariable Long id) {
        List<AlarmCriteriaLog> logs = criteriaService.selectLogsByCriteriaId(id);
        return success(logs);
    }
}
