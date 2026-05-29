package com.zwei.iot.device.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.ip.IpUtils;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.dto.DeviceAuthStatusChangeRequest;
import com.zwei.iot.device.domain.dto.DeviceAuthPasswordResetRequest;
import com.zwei.iot.device.domain.dto.DeviceCreateRequest;
import com.zwei.iot.device.domain.dto.DeviceUpdateRequest;
import com.zwei.iot.device.service.IDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public DeviceController(IDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 分页查询设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:list')")
    @GetMapping("/page")
    public AjaxResult page(Device device) {
        startPage();
        List<Device> list = deviceService.selectDevicePage(device, 0, 0);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        long total = new PageInfo(list).getTotal();
        HashMap<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", total);
        data.put("pageNum", pageDomain.getPageNum());
        data.put("pageSize", pageDomain.getPageSize());
        return AjaxResult.success("成功", data);
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
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:device:add')")
    @Log(title = "设备", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id) {
        Long newId = deviceService.copyDevice(id);
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
