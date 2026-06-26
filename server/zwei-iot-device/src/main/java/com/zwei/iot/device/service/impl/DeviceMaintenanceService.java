package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.service.IDeviceStatusLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备维保状态机服务 — 从 {@link DeviceServiceImpl} 中提取，单一职责。
 *
 * <h3>状态转换规则</h3>
 * <table>
 *   <tr><th>操作</th><th>类型</th><th>允许的原状态</th><th>目标状态</th></tr>
 *   <tr><td>报修</td><td>1</td><td>1 (正常)</td><td>2 (维修)</td></tr>
 *   <tr><td>修复</td><td>2</td><td>2 (维修)</td><td>1 (正常)</td></tr>
 *   <tr><td>停用</td><td>3</td><td>1 (正常) | 2 (维修)</td><td>3 (停用)</td></tr>
 *   <tr><td>恢复</td><td>4</td><td>3 (停用)</td><td>1 (正常)</td></tr>
 * </table>
 */
@Service
@Slf4j
class DeviceMaintenanceService {
    private final DeviceMapper deviceMapper;
    private final IDeviceStatusLogService deviceStatusLogService;

    DeviceMaintenanceService(DeviceMapper deviceMapper,
                             IDeviceStatusLogService deviceStatusLogService) {
        this.deviceMapper = deviceMapper;
        this.deviceStatusLogService = deviceStatusLogService;
    }

    /**
     * 设备维修状态操作（报修/修复/停用/启用）。
     * <p>在一个事务中完成状态变更 + 维修日志记录，保证原子性。</p>
     */
    @Transactional
    String maintenanceDevice(Long deviceId, Integer operationType, String operatorName, String operatorPhone,
                             String operationDate, String description, String createBy) {
        Device device = requireDevice(deviceId);
        int oldStatus = device.getStatus() != null ? device.getStatus() : 1;

        int newStatus = resolveAndValidateStatusTransition(operationType, oldStatus);

        String statusText = switch (operationType) {
            case 1 -> "报修";
            case 2 -> "修复";
            case 3 -> "停用";
            case 4 -> "启用";
            default -> throw new ServiceException("不支持的操作类型: " + operationType);
        };

        Device update = new Device();
        update.setId(deviceId);
        update.setStatus(newStatus);
        update.setUpdateBy(createBy);
        deviceMapper.updateDevice(update);

        deviceStatusLogService.saveMaintenanceLog(deviceId, device.getCode(), oldStatus, newStatus,
                statusText, operatorName, operatorPhone, operationDate, description, createBy);

        log.info("设备维修操作完成 deviceId={}, operation={}, {}→{}, operator={}",
                deviceId, statusText, oldStatus, newStatus, createBy);
        return statusText;
    }

    // ── private ──

    private Device requireDevice(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    /**
     * 解析操作类型并校验状态转换合法性。
     */
    private int resolveAndValidateStatusTransition(int operationType, int oldStatus) {
        return switch (operationType) {
            case 1 -> { // 报修：1 (正常) → 2 (维修)
                if (oldStatus != 1) throw new ServiceException("仅正常状态的设备可以报修");
                yield 2;
            }
            case 2 -> { // 修复：2 (维修) → 1 (正常)
                if (oldStatus != 2) throw new ServiceException("仅维修状态的设备可以修复");
                yield 1;
            }
            case 3 -> { // 停用：1 (正常) | 2 (维修) → 3 (停用)
                if (oldStatus != 1 && oldStatus != 2) throw new ServiceException("仅正常或维修状态的设备可以停用");
                yield 3;
            }
            case 4 -> { // 恢复：3 (停用) → 1 (正常)
                if (oldStatus != 3) throw new ServiceException("仅停用状态的设备可以恢复");
                yield 1;
            }
            default -> throw new ServiceException("不支持的操作类型: " + operationType);
        };
    }
}
