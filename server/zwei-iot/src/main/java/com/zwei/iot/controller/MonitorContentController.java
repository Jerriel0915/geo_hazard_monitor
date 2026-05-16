package com.zwei.iot.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.domain.MonitorContent;
import com.zwei.iot.service.IMonitorContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public MonitorContentController(IMonitorContentService monitorContentService) {
        this.monitorContentService = monitorContentService;
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
        return success(list);
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
            return error("监测内容不存在");
        }
        return success(monitorContent);
    }

    /**
     * 新增监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:add')")
    @Log(title = "监测内容", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MonitorContent monitorContent) {
        // 校验编码唯一性
        if (!monitorContentService.checkMonitorContentCodeUnique(monitorContent)) {
            return error("新增监测内容'" + monitorContent.getName() + "'失败，监测内容编码已存在");
        }
        // 设置创建者
        monitorContent.setCreateBy(getUsername());
        // 执行新增
        int rows = monitorContentService.insertMonitorContent(monitorContent);
        return rows > 0 ? success(monitorContent.getId()) : error("新增失败");
    }

    /**
     * 修改监测内容
     *
     * @param id             监测内容ID
     * @param monitorContent 监测内容信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorContent:edit')")
    @Log(title = "监测内容", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody MonitorContent monitorContent) {
        // 设置ID
        monitorContent.setId(id);
        // 校验编码唯一性
        if (!monitorContentService.checkMonitorContentCodeUnique(monitorContent)) {
            return error("修改监测内容'" + monitorContent.getName() + "'失败，监测内容编码已存在");
        }
        // 设置更新者
        monitorContent.setUpdateBy(getUsername());
        // 执行修改
        int rows = monitorContentService.updateMonitorContent(monitorContent);
        return rows > 0 ? success() : error("修改失败");
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
        int rows = monitorContentService.deleteMonitorContentById(id);
        return rows > 0 ? success() : error("删除失败");
    }
}