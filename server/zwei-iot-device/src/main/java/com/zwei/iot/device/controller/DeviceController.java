package com.zwei.iot.device.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceOnlineEventLog;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.DeviceStatusLog;
import com.zwei.iot.device.domain.dto.*;
import com.zwei.iot.device.service.IDeviceOnlineEventLogService;
import com.zwei.iot.device.service.IDeviceService;
import com.zwei.iot.device.service.IDeviceStatusLogService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备管理Controller
 * <p>
 * 提供设备的RESTful API接口，包括：
 * - 分页查询设备列表（GET /api/v1/devices/page）
 * - 获取设备详情及传感器（GET /api/v1/devices/{id}）
 * - 新增设备（POST /api/v1/devices）
 * - 修改设备（PUT /api/v1/devices/{id}）
 * - 删除设备（DELETE /api/v1/devices/{id}）
 * - 复制设备（POST /api/v1/devices/{id}/copy）
 * - 获取设备传感器列表（GET /api/v1/devices/{deviceId}/sensors）
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/devices")
public class DeviceController extends BaseController {
    private final IDeviceService deviceService;
    private final IDeviceStatusLogService deviceStatusLogService;
    private final IDeviceOnlineEventLogService onlineEventLogService;

    @Autowired
    public DeviceController(IDeviceService deviceService, IDeviceStatusLogService deviceStatusLogService,
                            IDeviceOnlineEventLogService onlineEventLogService) {
        this.deviceService = deviceService;
        this.deviceStatusLogService = deviceStatusLogService;
        this.onlineEventLogService = onlineEventLogService;
    }

    /**
     * 分页查询设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:list')")
    @GetMapping("/page")
    public AjaxResult page(Device device) {
        startPage();
        List<Device> list = deviceService.selectDevicePage(device, 0, 0);
        return pageResult(list);
    }

    /**
     * 导出设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:list')")
    @Log(title = "设备管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Device device) {
        List<Device> list = deviceService.selectDevicePage(device, 0, 0);
        List<DeviceExportVO> exportList = new ArrayList<>(list.size());
        for (Device item : list) {
            DeviceExportVO vo = new DeviceExportVO();
            vo.setCode(item.getCode());
            vo.setName(item.getName());
            vo.setSn(item.getSn());
            vo.setDeviceTypeName(item.getDeviceType() == null ? null
                    : item.getDeviceType() == 0 ? "单参数" : item.getDeviceType() == 1 ? "多参数" : "本地组网");
            vo.setNetworkTypeName(item.getNetworkType() == null ? null
                    : item.getNetworkType() == 0 ? "蜂窝" : "NB-Iot");
            vo.setProtocolType(item.getProtocolType());
            vo.setVendorName(item.getVendorName());
            vo.setLongitude(item.getLongitude());
            vo.setLatitude(item.getLatitude());
            vo.setStatusName(item.getStatusName());
            vo.setOnlineStatusName(item.getOnlineStatus() != null && item.getOnlineStatus() == 1 ? "在线" : "离线");
            vo.setSensorCount(item.getSensorCount());
            vo.setLastReportTime(item.getLastReportTime());
            vo.setCreateBy(item.getCreateBy());
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateBy(item.getUpdateBy());
            vo.setUpdateTime(item.getUpdateTime());
            exportList.add(vo);
        }
        ExcelUtil<DeviceExportVO> util = new ExcelUtil<>(DeviceExportVO.class);
        util.exportExcel(response, exportList, "设备数据");
    }

    /**
     * 获取所有设备列表（不分页）
     *
     * @param device 查询条件
     * @return 设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:list')")
    @GetMapping
    public AjaxResult list(Device device) {
        List<Device> list = deviceService.selectDeviceAll();
        return success(list);
    }

    /**
     * 获取设备详情及传感器
     *
     * @param id 设备ID
     * @return 设备详情
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        Device device = deviceService.selectDeviceById(id);
        if (device == null) {
            return error("设备不存在");
        }
        return success(device);
    }

    /**
     * 新增设备
     *
     * @param request 设备信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DeviceCreateRequest request) {
        Device device = deviceService.createDevice(request, getUsername());
        return AjaxResult.success("新增成功", Map.of(
                "id", device.getId(),
                "username", device.getAuthUsername(),
                "password", device.getAuthPassword()
        ));
    }

    /**
     * 修改设备
     *
     * @param id     设备ID
     * @param request 设备信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:edit')")
    @Log(title = "设备", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody DeviceUpdateRequest request) {
        Device device = deviceService.updateDevice(id, request, getUsername());
        return AjaxResult.success("修改成功", Map.of("id", device.getId()));
    }

    /**
     * 删除设备（逻辑删除）
     *
     * @param id 设备ID
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:remove')")
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = deviceService.deleteDeviceById(id);
        return rows > 0 ? success() : error("删除失败");
    }

    /**
     * 复制设备
     *
     * @param id 设备ID
     * @param request 复制请求（含新编号、名称）
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id,
                           @Validated @RequestBody DeviceCopyRequest request) {
        Long newId = deviceService.copyDevice(id, request);
        if (newId == null) {
            return error("复制失败，设备不存在");
        }
        return success(newId);
    }

    /**
     * 获取设备传感器列表
     *
     * @param deviceId 设备ID
     * @return 传感器列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/{deviceId}/sensors")
    public AjaxResult getSensors(@PathVariable Long deviceId) {
        List<DeviceSensor> sensors = deviceService.selectSensorListByDeviceId(deviceId);
        return success(sensors);
    }

    /**
     * 查看设备账号
     */
    @PreAuthorize("@ss.hasPermi('basic:device:auth:view')")
    @GetMapping("/{id}/auth-account")
    public AjaxResult getAuthAccount(@PathVariable Long id) {
        Device device = deviceService.getDeviceAuthAccount(id, getUsername(), IpUtils.getIpAddr());
        return AjaxResult.success("成功", buildAuthAccount(device));
    }

    /**
     * 重置设备密码
     */
    @PreAuthorize("@ss.hasPermi('basic:device:auth:reset')")
    @Log(title = "设备账号", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/auth-password/reset")
    public AjaxResult resetPassword(@PathVariable Long id,
                                    @RequestBody(required = false) DeviceAuthPasswordResetRequest request) {
        String reason = request == null ? null : request.getReason();
        Boolean forceOffline = request == null ? null : request.getForceOffline();
        Device device = deviceService.resetDeviceAuthPassword(id, getUsername(), reason, forceOffline, IpUtils.getIpAddr());
        return AjaxResult.success("重置成功", Map.of(
                "username", device.getAuthUsername(),
                "password", device.getAuthPassword()
        ));
    }

    /**
     * 启停设备账号
     */
    @PreAuthorize("@ss.hasPermi('basic:device:auth:status')")
    @Log(title = "设备账号", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/auth-status")
    public AjaxResult changeAuthStatus(@PathVariable Long id,
                                       @Validated @RequestBody DeviceAuthStatusChangeRequest request) {
        Device device = deviceService.changeDeviceAuthStatus(
                id,
                request.getAuthStatus(),
                getUsername(),
                request.getReason(),
                IpUtils.getIpAddr()
        );
        return AjaxResult.success("状态更新成功", buildAuthAccount(device));
    }

    /**
     * 设备维修状态操作（报修/修复/停用/恢复）
     */
    @PreAuthorize("@ss.hasPermi('basic:device:edit')")
    @Log(title = "设备维修", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/maintenance")
    public AjaxResult maintenance(@PathVariable Long id, @Validated @RequestBody DeviceMaintenanceRequest req) {
        String statusText = deviceService.maintenanceDevice(id, req.getOperationType(),
                req.getOperatorName(), req.getOperatorPhone(),
                req.getOperationDate(), req.getDescription(), getUsername());
        return AjaxResult.success(statusText + "成功");
    }

    /**
     * 获取设备维修记录
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/{id}/maintenance-logs")
    public AjaxResult maintenanceLogs(@PathVariable Long id) {
        List<DeviceStatusLog> logs = deviceStatusLogService.getLogsByDeviceId(id);
        return AjaxResult.success("成功", logs);
    }

    /**
     * 获取设备上下线记录
     */
    @PreAuthorize("@ss.hasPermi('basic:device:query')")
    @GetMapping("/{id}/online-logs")
    public AjaxResult onlineLogs(@PathVariable Long id) {
        List<DeviceOnlineEventLog> logs = onlineEventLogService.selectByDeviceId(id, 50);
        return AjaxResult.success("成功", logs);
    }

    private Map<String, Object> buildAuthAccount(Device device) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("deviceId", device.getId());
        data.put("username", device.getAuthUsername());
        data.put("password", device.getAuthPassword());
        data.put("authStatus", device.getAuthStatus());
        data.put("registeredAt", device.getRegisteredAt());
        data.put("lastAuthTime", device.getLastAuthTime());
        data.put("lastAuthIp", device.getLastAuthIp());
        return data;
    }
}
