package com.zwei.module.iot.device.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.module.iot.device.domain.DeviceAliveLog;
import com.zwei.module.iot.device.service.IDeviceAliveLogService;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.common.core.page.TableDataInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;

/**
 * 状态日志Controller
 * 
 * @author linx
 * @date 2025-09-05
 */
@Api(tags = "状态日志管理")
@RestController
@RequestMapping("/device/deviceAliveLog")
public class DeviceAliveLogController extends BaseController
{
    @Autowired
    private IDeviceAliveLogService deviceAliveLogService;

    /**
     * 查询状态日志列表
     */
    @ApiOperation("获取状态日志列表")
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceAliveLog deviceAliveLog)
    {
        startPage();
        List<DeviceAliveLog> list = deviceAliveLogService.selectDeviceAliveLogList(deviceAliveLog);
        return getDataTable(list);
    }

    /**
     * 导出状态日志列表
     */
    @ApiOperation("导出状态日志列表")
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:export')")
    @Log(title = "状态日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceAliveLog deviceAliveLog)
    {
        List<DeviceAliveLog> list = deviceAliveLogService.selectDeviceAliveLogList(deviceAliveLog);
        ExcelUtil<DeviceAliveLog> util = new ExcelUtil<DeviceAliveLog>(DeviceAliveLog.class);
        util.exportExcel(response, list, "状态日志数据");
    }

    /**
     * 获取状态日志详细信息
     */
    @ApiOperation("获取状态日志详细信息")
    @ApiImplicitParam(name = "id", value = "日志ID", required = true, dataType = "Long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(deviceAliveLogService.selectDeviceAliveLogById(id));
    }

    /**
     * 新增状态日志
     */
    @ApiOperation("新增状态日志")
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:add')")
    @Log(title = "状态日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DeviceAliveLog deviceAliveLog)
    {
        return toAjax(deviceAliveLogService.insertDeviceAliveLog(deviceAliveLog));
    }

    /**
     * 修改状态日志
     */
    @ApiOperation("修改状态日志")
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:edit')")
    @Log(title = "状态日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DeviceAliveLog deviceAliveLog)
    {
        return toAjax(deviceAliveLogService.updateDeviceAliveLog(deviceAliveLog));
    }

    /**
     * 删除状态日志
     */
    @ApiOperation("删除状态日志")
    @ApiImplicitParam(name = "ids", value = "日志ID列表", required = true, dataType = "Long[]", paramType = "path", dataTypeClass = Long[].class)
    @PreAuthorize("@ss.hasPermi('device:deviceAliveLog:remove')")
    @Log(title = "状态日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(deviceAliveLogService.deleteDeviceAliveLogByIds(ids));
    }
}