package com.zwei.iot.device.controller;

import com.zwei.common.annotation.Anonymous;
import com.zwei.common.annotation.RateLimiter;
import com.zwei.common.annotation.RepeatSubmit;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.LimitType;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.dto.DeviceRegisterRequest;
import com.zwei.iot.device.domain.vo.DeviceRegistryResult;
import com.zwei.iot.device.service.IDeviceRegistryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 设备注册中心
 */
@RestController
@RequestMapping("api/v1/device-registry")
public class DeviceRegistryController {
    private final IDeviceRegistryService deviceRegistryService;

    public DeviceRegistryController(IDeviceRegistryService deviceRegistryService) {
        this.deviceRegistryService = deviceRegistryService;
    }

    @Anonymous
    @RepeatSubmit
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping("/register")
    public AjaxResult register(@Valid @RequestBody DeviceRegisterRequest request) {
        try {
            DeviceRegistryResult result = deviceRegistryService.register(request);
            return AjaxResult.success("注册成功", Map.of(
                    "deviceId", result.device().getId(),
                    "username", result.device().getAuthUsername(),
                    "password", result.device().getAuthPassword(),
                    "created", result.created()
            ));
        } catch (ServiceException ex) {
            Integer code = ex.getCode() == null ? 500 : ex.getCode();
            return AjaxResult.error(code, ex.getMessage());
        }
    }
}
