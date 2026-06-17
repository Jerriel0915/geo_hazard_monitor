package com.zwei.iot.monitor.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.constant.HttpStatus;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.dto.MonitorContentCreateRequest;
import com.zwei.iot.monitor.domain.dto.MonitorContentUpdateRequest;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 监测内容管理Controller
 * <p>
 * 提供监测内容的RESTful API接口，包括：
 * - 获取监测内容列表（GET /api/v1/monitor-contents）
 * - 获取监测内容详情（GET /api/v1/monitor-contents/{id}）
 * - 新增监测内容（POST /api/v1/monitor-contents）
 * - 修改监测内容（PUT /api/v1/monitor-contents/{id}）
 * - 删除监测内容（DELETE /api/v1/monitor-contents/{id}）
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/monitor-contents")
public class MonitorContentController extends BaseController {

    /**
     * 注入监测内容Service
     */
    private final IMonitorContentService monitorContentService;
    private final IMonitorTypeService monitorTypeService;

    @Autowired
    public MonitorContentController(IMonitorContentService monitorContentService,
                                    IMonitorTypeService monitorTypeService) {
        this.monitorContentService = monitorContentService;
        this.monitorTypeService = monitorTypeService;
    }

    /**
     * 获取监测内容列表
     * <p>
     * 支持通过monitorTypeId参数过滤指定监测类型下的监测内容。
     *
     * @param monitorTypeId 监测类型ID（可选）
     * @return 监测内容列表
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:list')")
    @GetMapping
    public AjaxResult list(@RequestParam(required = false) Long monitorTypeId) {
        List<MonitorContent> list = monitorContentService.selectMonitorContentAll(monitorTypeId);
        return AjaxResult.success("成功", list);
    }

    /**
     * 获取监测内容详情
     *
     * @param id 监测内容ID
     * @return 监测内容详情
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        MonitorContent monitorContent = monitorContentService.selectMonitorContentById(id);
        if (monitorContent == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "监测内容不存在");
        }
        return AjaxResult.success("成功", monitorContent);
    }

    /**
     * 新增监测内容
     *
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:add')")
    @Log(title = "监测内容", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MonitorContentCreateRequest request) {
        if (monitorTypeService.selectMonitorTypeById(request.getMonitorTypeId()) == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "监测类型不存在");
        }
        // fieldType 默认 inherent;computed 时 calcScript 必填
        String fieldType = request.getFieldType() == null ? "inherent" : request.getFieldType();
        if ("computed".equals(fieldType)
                && (request.getCalcScript() == null || request.getCalcScript().isBlank())) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "计算属性必须填写计算脚本");
        }
        if (!isValidRange(request.getRangeMin(), request.getRangeMax())) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "量程范围不合法，最大值不能小于最小值");
        }
        MonitorContent monitorContent = buildMonitorContentForCreate(request);
        // 校验编码唯一性
        if (!monitorContentService.checkMonitorContentCodeUnique(monitorContent)) {
            return error("新增监测内容'" + monitorContent.getName() + "'失败，监测内容编码已存在");
        }
        // 设置创建者
        monitorContent.setCreateBy(getUsername());
        // 执行新增
        int rows = monitorContentService.insertMonitorContent(monitorContent);
        return rows > 0
                ? AjaxResult.success("新增成功", Collections.singletonMap("id", monitorContent.getId()))
                : error("新增失败");
    }

    /**
     * 修改监测内容
     *
     * @param id      监测内容ID
     * @param request 修改请求参数
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:edit')")
    @Log(title = "监测内容", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody MonitorContentUpdateRequest request) {
        if (!request.hasUpdatableField()) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "修改失败，请至少提供一个可更新字段");
        }
        MonitorContent current = monitorContentService.selectMonitorContentById(id);
        if (current == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "监测内容不存在");
        }
        BigDecimal effectiveRangeMin = request.getRangeMin() != null ? request.getRangeMin() : current.getRangeMin();
        BigDecimal effectiveRangeMax = request.getRangeMax() != null ? request.getRangeMax() : current.getRangeMax();
        if (!isValidRange(effectiveRangeMin, effectiveRangeMax)) {
            return AjaxResult.error(HttpStatus.BAD_REQUEST, "量程范围不合法，最大值不能小于最小值");
        }
        MonitorContent monitorContent = buildMonitorContentForUpdate(id, request);
        // 设置更新者
        monitorContent.setUpdateBy(getUsername());
        // 执行修改
        int rows = monitorContentService.updateMonitorContent(monitorContent);
        return rows > 0 ? AjaxResult.success("修改成功") : error("修改失败");
    }

    /**
     * 删除监测内容（逻辑删除）
     *
     * @param id 监测内容ID
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:remove')")
    @Log(title = "监测内容", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        if (monitorContentService.selectMonitorContentById(id) == null) {
            return AjaxResult.error(HttpStatus.NOT_FOUND, "监测内容不存在");
        }
        int rows = monitorContentService.deleteMonitorContentById(id);
        return rows > 0 ? AjaxResult.success("删除成功") : error("删除失败");
    }

    private MonitorContent buildMonitorContentForCreate(MonitorContentCreateRequest request) {
        String fieldType = request.getFieldType() == null ? "inherent" : request.getFieldType();
        MonitorContent monitorContent = new MonitorContent();
        monitorContent.setMonitorTypeId(request.getMonitorTypeId());
        monitorContent.setCode(request.getCode());
        monitorContent.setName(request.getName());
        monitorContent.setUnit(request.getUnit());
        monitorContent.setSortOrder(request.getSortOrder());
        monitorContent.setIndicatorType(request.getIndicatorType());
        monitorContent.setIcon(request.getIcon());
        monitorContent.setRangeMin(request.getRangeMin());
        monitorContent.setRangeMax(request.getRangeMax());
        monitorContent.setFieldType(fieldType);
        monitorContent.setCalcScript(request.getCalcScript());
        return monitorContent;
    }

    private MonitorContent buildMonitorContentForUpdate(Long id, MonitorContentUpdateRequest request) {
        MonitorContent monitorContent = new MonitorContent();
        monitorContent.setId(id);
        monitorContent.setName(request.getName());
        monitorContent.setUnit(request.getUnit());
        if (request.getSortOrder() != null) {
            monitorContent.setSortOrder(request.getSortOrder());
        }
        monitorContent.setIcon(request.getIcon());
        monitorContent.setRangeMin(request.getRangeMin());
        monitorContent.setRangeMax(request.getRangeMax());
        monitorContent.setCalcScript(request.getCalcScript());
        return monitorContent;
    }

    private boolean isValidRange(BigDecimal rangeMin, BigDecimal rangeMax) {
        return rangeMin == null || rangeMax == null || rangeMax.compareTo(rangeMin) >= 0;
    }
}
