package com.zwei.monitor.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.monitor.domain.MonitoringObject;
import com.zwei.monitor.service.IMonitoringObjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 监测对象基本信息表Controller
 * 
 * @author zwei
 * @date 2025-10-15
 */
@Api(tags = "监测对象管理")
@Slf4j
@RestController
@RequestMapping("/monitor/object")
public class MonitoringObjectController extends BaseController
{
    @Autowired
    private IMonitoringObjectService monitoringObjectService;

    /**
     * 查询监测对象基本信息表列表
     */
    @ApiOperation("获取监测对象基本信息表列表")
    @PreAuthorize("@ss.hasPermi('monitor:object:list')")
    @GetMapping("/list")
    public TableDataInfo list(MonitoringObject monitoringObject)
    {
        startPage();
        List<MonitoringObject> list = monitoringObjectService.selectMonitoringObjectList(monitoringObject);
        return getDataTable(list);
    }

    /**
     * 导出监测对象基本信息表列表
     */
    @ApiOperation("导出监测对象基本信息表列表")
    @PreAuthorize("@ss.hasPermi('monitor:object:export')")
    @Log(title = "监测对象基本信息表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MonitoringObject monitoringObject)
    {
        List<MonitoringObject> list = monitoringObjectService.selectMonitoringObjectList(monitoringObject);
        ExcelUtil<MonitoringObject> util = new ExcelUtil<MonitoringObject>(MonitoringObject.class);
        util.exportExcel(response, list, "监测对象基本信息表数据");
    }

    /**
     * 获取监测对象基本信息表详细信息
     */
    @ApiOperation("获取监测对象基本信息表详细信息")
    @PreAuthorize("@ss.hasPermi('monitor:object:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(monitoringObjectService.selectMonitoringObjectById(id));
    }

    /**
     * 新增监测对象基本信息表
     */
    @ApiOperation("新增监测对象基本信息表")
    @PreAuthorize("@ss.hasPermi('monitor:object:add')")
    @Log(title = "监测对象基本信息表", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MonitoringObject monitoringObject)
    {
        return toAjax(monitoringObjectService.insertMonitoringObject(monitoringObject));
    }

    /**
     * 修改监测对象基本信息表
     */
    @ApiOperation("修改监测对象基本信息表")
    @PreAuthorize("@ss.hasPermi('monitor:object:edit')")
    @Log(title = "监测对象基本信息表", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MonitoringObject monitoringObject)
    {
        return toAjax(monitoringObjectService.updateMonitoringObject(monitoringObject));
    }

    /**
     * 删除监测对象基本信息表
     */
    @ApiOperation("删除监测对象基本信息表")
    @PreAuthorize("@ss.hasPermi('monitor:object:remove')")
    @Log(title = "监测对象基本信息表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(monitoringObjectService.deleteMonitoringObjectByIds(ids));
    }
}