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
import com.zwei.module.iot.device.domain.DeviceStatus;
import com.zwei.module.iot.device.service.IDeviceStatusService;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.common.core.page.TableDataInfo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;

/**
 * 设备状态Controller
 * 
 * @author linx
 * @date 2025-09-05
 */
@Api(tags = "设备状态管理")
@RestController
@RequestMapping("/device/deviceStatus")
public class DeviceStatusController extends BaseController
{
    @Autowired
    private IDeviceStatusService deviceStatusService;

    /**
     * 查询设备状态列表
     */
    @ApiOperation("获取设备状态列表")
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceStatus deviceStatus)
    {
        startPage();
        List<DeviceStatus> list = deviceStatusService.selectDeviceStatusList(deviceStatus);
        return getDataTable(list);
    }

    /**
     * 导出设备状态列表
     */
    @ApiOperation("导出设备状态列表")
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:export')")
    @Log(title = "设备状态", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceStatus deviceStatus)
    {
        List<DeviceStatus> list = deviceStatusService.selectDeviceStatusList(deviceStatus);
        ExcelUtil<DeviceStatus> util = new ExcelUtil<DeviceStatus>(DeviceStatus.class);
        util.exportExcel(response, list, "设备实时状态数据");
    }

    /**
     * 获取设备状态详细信息
     */
    @ApiOperation("获取设备状态详细信息")
    @ApiImplicitParam(name = "deviceId", value = "设备ID", required = true, dataType = "Long", paramType = "path", dataTypeClass = Long.class)
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable("deviceId") Long deviceId)
    {
        return success(deviceStatusService.selectDeviceStatusByDeviceId(deviceId.toString()));
    }

    /**
     * 新增设备状态
     */
    @ApiOperation("新增设备状态")
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:add')")
    @Log(title = "设备状态", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody DeviceStatus deviceStatus)
    {
        return toAjax(deviceStatusService.insertDeviceStatus(deviceStatus));
    }

    /**
     * 修改设备状态
     */
    @ApiOperation("修改设备状态")
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:edit')")
    @Log(title = "设备状态", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody DeviceStatus deviceStatus)
    {
        return toAjax(deviceStatusService.updateDeviceStatus(deviceStatus));
    }

    /**
     * 删除设备状态
     */
    @ApiOperation("删除设备状态")
    @ApiImplicitParam(name = "deviceIds", value = "设备ID列表", required = true, dataType = "Long[]", paramType = "path", dataTypeClass = Long[].class)
    @PreAuthorize("@ss.hasPermi('device:deviceStatus:remove')")
    @Log(title = "设备状态", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds)
    {
        return toAjax(deviceStatusService.deleteDeviceStatusByDeviceIds(deviceIds));
    }
}