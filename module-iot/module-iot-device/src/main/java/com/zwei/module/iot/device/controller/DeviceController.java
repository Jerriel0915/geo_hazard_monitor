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
import com.zwei.module.iot.device.domain.Device;
import com.zwei.module.iot.device.service.IDeviceService;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.common.core.page.TableDataInfo;

/**
 * 设备基本信息Controller
 * 
 * @author zwei
 * @date 2025-09-05
 */
@RestController
@RequestMapping("/device/device")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    /**
     * 查询设备基本信息列表
     */
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
    @PreAuthorize("@ss.hasPermi('device:device:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(deviceService.selectDeviceById(id));
    }

    /**
     * 新增设备基本信息
     */
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
    @PreAuthorize("@ss.hasPermi('device:device:remove')")
    @Log(title = "设备基本信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }
}
