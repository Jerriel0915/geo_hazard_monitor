package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.domain.AlarmFeedback;
import com.zwei.iot.alarm.domain.AlarmRecord;
import com.zwei.iot.alarm.domain.AlarmRecordLog;
import com.zwei.iot.alarm.domain.dto.AlarmRecordDisposeRequest;
import com.zwei.iot.alarm.domain.dto.BatchDisposeRequest;
import com.zwei.iot.alarm.service.IAlarmFeedbackService;
import com.zwei.iot.alarm.service.IAlarmRecordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 告警记录管理 Controller
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/records")
public class AlarmRecordController extends BaseController {

    private final IAlarmRecordService alarmRecordService;
    private final IAlarmFeedbackService alarmFeedbackService;

    public AlarmRecordController(IAlarmRecordService alarmRecordService,
                                 IAlarmFeedbackService alarmFeedbackService) {
        this.alarmRecordService = alarmRecordService;
        this.alarmFeedbackService = alarmFeedbackService;
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
        return toAjax(alarmRecordService.dispose(id, request.getStatus(), request.getNote(), getUsername()));
    }

    @PostMapping("/batch")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:batch')")
    public AjaxResult batchDispose(@RequestBody BatchDisposeRequest request) {
        return toAjax(alarmRecordService.batchDispose(
                request.getIds().toArray(new Long[0]),
                request.getStatus(),
                getUsername()));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult getLogs(@PathVariable Long id) {
        List<AlarmRecordLog> logs = alarmRecordService.selectLogsByAlarmId(id);
        return success(logs);
    }

    @GetMapping("/{id}/feedbacks")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:list')")
    public AjaxResult getFeedbacks(@PathVariable Long id) {
        List<AlarmFeedback> feedbacks = alarmFeedbackService.getFeedbacksByAlarmId(id);
        return success(feedbacks);
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("@ss.hasPermi('iot:alarm-record:dispose')")
    public AjaxResult addFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = body.get("content") != null ? body.get("content").toString() : "";
        String files = body.get("files") != null ? body.get("files").toString() : null;
        alarmFeedbackService.addFeedback(id, content, files, getUsername());
        return success();
    }
}
