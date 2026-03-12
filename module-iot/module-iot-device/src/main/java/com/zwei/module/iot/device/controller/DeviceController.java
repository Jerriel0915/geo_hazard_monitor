package com.zwei.module.iot.device.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.service.IDeviceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 设备基本信息Controller
 * 
 * @author zwei
 * @date 2025-09-05
 */
@Api(tags = "设备基本信息管理")
@RestController
@RequestMapping("/device/device")
public class DeviceController extends BaseController
{
    private final IDeviceService deviceService;

    @Autowired
    DeviceController(IDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 查询设备基本信息列表
     */
    @ApiOperation("获取设备基本信息列表")
    @PreAuthorize("@ss.hasPermi('device:device:list')")
    @GetMapping("/list")
    public TableDataInfo list(Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 导出设备基本信息列表
     */
    @ApiOperation("导出设备基本信息列表")
    @PreAuthorize("@ss.hasPermi('device:device:export')")
    @Log(title = "设备基本信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Device device)
    {
        List<Device> list = deviceService.selectDeviceList(device);
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device.class);
        util.exportExcel(response, list, "设备基本信息数据");
    }

    /**
     * 获取设备基本信息详细信息
     */
    @ApiOperation("获取设备基本信息详细信息")
    @ApiImplicitParam(name = "id", value = "设备ID", required = true, dataType = "Long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('device:device:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(deviceService.selectDeviceById(id));
    }

    /**
     * 新增设备基本信息
     */
    @ApiOperation("新增设备基本信息")
    @PreAuthorize("@ss.hasPermi('device:device:add')")
    @Log(title = "设备基本信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Device device)
    {
        return toAjax(deviceService.insertDevice(device));
    }

    /**
     * 修改设备基本信息
     */
    @ApiOperation("修改设备基本信息")
    @PreAuthorize("@ss.hasPermi('device:device:edit')")
    @Log(title = "设备基本信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Device device)
    {
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备基本信息
     */
    @ApiOperation("删除设备基本信息")
    @ApiImplicitParam(name = "ids", value = "设备ID列表", required = true, dataType = "Long[]", paramType = "path", dataTypeClass = Long[].class)
    @PreAuthorize("@ss.hasPermi('device:device:remove')")
    @Log(title = "设备基本信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }
}
