package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceAuthLog;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.DeviceAuthLogService;
import com.zwei.iot.device.service.IDeviceSessionService;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 设备认证账号管理服务 — 从 {@link DeviceServiceImpl} 中提取，单一职责。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>查看设备接入账号</li>
 *   <li>重置设备密码（支持 forceOffline 强制断连）</li>
 *   <li>启停设备认证状态</li>
 *   <li>操作审计日志记录</li>
 * </ul>
 */
@Service
@Slf4j
class DeviceAuthService {
    private static final int AUTH_STATUS_ENABLED = 1;
    private static final int AUTH_STATUS_DISABLED = 2;

    private final DeviceMapper deviceMapper;
    private final DeviceAuthAccountGenerator accountGenerator;
    private final DeviceAuthLogService deviceAuthLogService;
    private final ObjectProvider<IDeviceSessionService> deviceSessionServiceProvider;

    DeviceAuthService(DeviceMapper deviceMapper,
                      DeviceAuthAccountGenerator accountGenerator,
                      DeviceAuthLogService deviceAuthLogService,
                      ObjectProvider<IDeviceSessionService> deviceSessionServiceProvider) {
        this.deviceMapper = deviceMapper;
        this.accountGenerator = accountGenerator;
        this.deviceAuthLogService = deviceAuthLogService;
        this.deviceSessionServiceProvider = deviceSessionServiceProvider;
    }

    Device getDeviceAuthAccount(Long deviceId, String operator, String clientIp) {
        Device device = requireDevice(deviceId);
        saveAuthAuditLog(device, operator, clientIp, true, "WEB_VIEW_ACCOUNT");
        return device;
    }

    @Transactional
    Device resetDeviceAuthPassword(Long deviceId, String operator, String resetReason, Boolean forceOffline, String clientIp) {
        Device current = requireDevice(deviceId);
        String password = accountGenerator.generatePassword();
        Device update = new Device();
        update.setId(deviceId);
        update.setAuthPassword(password);
        update.setUpdateBy(operator);
        deviceMapper.updateDevice(update);
        Device latest = requireDevice(deviceId);
        latest.setAuthUsername(current.getAuthUsername());
        latest.setLastAuthTime(current.getLastAuthTime());
        latest.setLastAuthIp(current.getLastAuthIp());
        latest.setAuthPassword(password);
        saveAuthAuditLog(latest, operator, clientIp, true, buildResetPasswordDetail(resetReason, forceOffline));
        if (Boolean.TRUE.equals(forceOffline)) {
            IDeviceSessionService sessionService = deviceSessionServiceProvider.getIfAvailable();
            if (sessionService != null) {
                boolean disconnected = sessionService.disconnectDevice(deviceId);
                log.info("密码重置后强制断连 deviceId={}, result={}", deviceId, disconnected);
            } else {
                log.warn("IDeviceSessionService 不可用，跳过 MQTT 断连 deviceId={}", deviceId);
            }
        }
        return latest;
    }

    @Transactional
    Device changeDeviceAuthStatus(Long deviceId, Integer authStatus, String operator, String reason, String clientIp) {
        Device current = requireDevice(deviceId);
        validateAuthStatus(authStatus);
        if (Objects.equals(current.getAuthStatus(), authStatus)) {
            saveAuthAuditLog(current, operator, clientIp, true, buildAuthStatusDetail(authStatus, reason, true));
            return current;
        }
        Device update = new Device();
        update.setId(deviceId);
        update.setAuthStatus(authStatus);
        update.setUpdateBy(operator);
        deviceMapper.updateDevice(update);
        Device latest = requireDevice(deviceId);
        saveAuthAuditLog(latest, operator, clientIp, true, buildAuthStatusDetail(authStatus, reason, false));
        return latest;
    }

    // ── private helpers ──

    private Device requireDevice(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    private void validateAuthStatus(Integer authStatus) {
        if (!Objects.equals(authStatus, AUTH_STATUS_ENABLED) && !Objects.equals(authStatus, AUTH_STATUS_DISABLED)) {
            throw new ServiceException("账号状态非法");
        }
    }

    private void saveAuthAuditLog(Device device, String operator, String clientIp, boolean success, String detail) {
        if (device == null || device.getId() == null || device.getAuthUsername() == null) {
            return;
        }
        DeviceAuthLog log = new DeviceAuthLog();
        log.setDeviceId(device.getId());
        log.setAuthUsername(device.getAuthUsername());
        log.setAuthResult(success ? 1 : 0);
        log.setClientId(limitLength(normalizeOptional(operator), 128));
        log.setClientIp(limitLength(normalizeOptional(clientIp), 64));
        log.setFailureReason(limitLength(detail, 255));
        deviceAuthLogService.save(log);
    }

    private String buildResetPasswordDetail(String resetReason, Boolean forceOffline) {
        StringBuilder builder = new StringBuilder("WEB_RESET_PASSWORD");
        if (resetReason != null && !resetReason.isBlank()) {
            builder.append("|reason=").append(resetReason.trim());
        }
        if (forceOffline != null) {
            builder.append("|forceOffline=").append(forceOffline);
        }
        return builder.toString();
    }

    private String buildAuthStatusDetail(Integer authStatus, String reason, boolean unchanged) {
        StringBuilder builder = new StringBuilder("WEB_CHANGE_AUTH_STATUS");
        builder.append("|target=").append(authStatus);
        if (unchanged) {
            builder.append("|unchanged=true");
        }
        if (reason != null && !reason.isBlank()) {
            builder.append("|reason=").append(reason.trim());
        }
        return builder.toString();
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
