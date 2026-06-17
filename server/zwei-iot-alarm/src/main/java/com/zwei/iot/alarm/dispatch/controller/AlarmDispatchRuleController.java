package com.zwei.iot.alarm.dispatch.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleCreateRequest;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleDetailVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleItemVO;
import com.zwei.iot.alarm.dispatch.dto.AlarmDispatchRuleQuery;
import com.zwei.iot.alarm.dispatch.service.IAlarmDispatchRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知规则 CRUD 控制器
 */
@RestController
@RequestMapping("/api/v1/alarm/dispatch")
public class AlarmDispatchRuleController extends BaseController {

    @Autowired
    private IAlarmDispatchRuleService service;

    /** 分页列表（全量返回，前端客户端分页） */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/list")
    public TableDataInfo list(AlarmDispatchRuleQuery query) {
        List<AlarmDispatchRuleItemVO> list = service.selectList(query);
        return getDataTable(list);
    }

    /** 详情 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        AlarmDispatchRuleDetailVO vo = service.selectDetail(id);
        return AjaxResult.success(vo);
    }

    /** 新增 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:add')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody AlarmDispatchRuleCreateRequest req) {
        return toAjax(service.create(req));
    }

    /** 编辑 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:edit')")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id,
                              @Valid @RequestBody AlarmDispatchRuleCreateRequest req) {
        req.setId(id);
        return toAjax(service.update(id, req));
    }

    /** 删除 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(service.delete(id));
    }

    /** 启用/禁用 */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:edit')")
    @PutMapping("/{id}/enabled")
    public AjaxResult toggleEnabled(@PathVariable Long id,
                                     @RequestBody Map<String, Object> body) {
        Integer isEnabled = (Integer) body.get("isEnabled");
        return toAjax(service.toggleEnabled(id, isEnabled));
    }

    /** 接收人选项（前端勾选用） */
    @PreAuthorize("@ss.hasPermi('alarm:dispatch:list')")
    @GetMapping("/recipient-options")
    public AjaxResult recipientOptions() {
        return AjaxResult.success(service.selectRecipientOptions());
    }
}
