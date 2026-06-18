package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordActionLog;
import com.zwei.iot.alarm.domain.AlarmRecordTriggerDetail;
import com.zwei.iot.alarm.domain.dto.AlarmRecordDisposeRequest;
import com.zwei.iot.alarm.domain.dto.BatchDisposeRequest;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警记录管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/records")
public class AlarmRecordController extends BaseController {

    private final IAlarmRecordService alarmRecordService;
    private final IAlarmNotificationService notificationService;

    public AlarmRecordController(IAlarmRecordService alarmRecordService,
                                 IAlarmNotificationService notificationService) {
        this.alarmRecordService = alarmRecordService;
        this.notificationService = notificationService;
    }

    @GetMapping("/pending")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public TableDataInfo pending(AlarmRecord record) {
        startPage();
        List<AlarmRecord> list = alarmRecordService.selectPendingList(record);
        return getDataTable(list);
    }

    @GetMapping("/history")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public TableDataInfo history(AlarmRecord record) {
        startPage();
        List<AlarmRecord> list = alarmRecordService.selectHistoryList(record);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult getById(@PathVariable Long id) {
        return success(alarmRecordService.selectById(id));
    }

    @PutMapping("/{id}/dispose")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:dispose')")
    public AjaxResult dispose(@PathVariable Long id, @RequestBody AlarmRecordDisposeRequest request) {
        return toAjax(alarmRecordService.dispose(
                id,
                request.getStatus(),
                request.getDescription(),
                request.getAttachments(),
                request.getRemarks() != null ? request.getRemarks() : request.getNote(),
                getUsername()));
    }

    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:batch')")
    public AjaxResult batchDispose(@RequestBody BatchDisposeRequest request) {
        return toAjax(alarmRecordService.batchDispose(
                request.getIds().toArray(new Long[0]),
                request.getStatus(),
                request.getDescription(),
                request.getAttachments(),
                request.getRemarks() != null ? request.getRemarks() : request.getNote(),
                getUsername()));
    }

    /** 触发明细列表 (告警记录 tab) */
    @GetMapping("/{id}/trigger-details")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult triggerDetails(@PathVariable Long id) {
        List<AlarmRecordTriggerDetail> details = alarmRecordService.selectTriggerDetailsByAlarmRecordId(id);
        return success(details);
    }

    /** 动作日志列表 (处置记录 tab + 时间线) */
    @GetMapping("/{id}/action-logs")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult actionLogs(@PathVariable Long id) {
        List<AlarmRecordActionLog> logs = alarmRecordService.selectActionLogsByAlarmRecordId(id);
        return success(logs);
    }

    /** 通知记录列表 (通知记录 tab) */
    @GetMapping("/{id}/notifications")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult notifications(@PathVariable Long id) {
        return success(notificationService.selectByAlarmId(id));
    }
}
