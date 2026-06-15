package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceAuthLog;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.dto.DeviceCreateRequest;
import com.zwei.iot.device.domain.dto.DeviceUpdateRequest;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.*;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 设备全生命周期管理服务。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>设备 CRUD</b>：创建（自动生成认证账号）、更新、删除（级联传感器+属性+绑定关系）</li>
 *   <li><b>设备复制</b>：深拷贝设备及其下所有传感器和属性（sensorCode 加 _copy 后缀防冲突）</li>
 *   <li><b>认证账号管理</b>：查看/重置密码（支持 forceOffline 强制断连）、启停认证状态</li>
 *   <li><b>大规模离线判定</b>：设备超过阈值时间未上报 → 标记为离线并记录审计日志</li>
 * </ul>
 *
 * <h3>账号生成规则</h3>
 * 由 {@link DeviceAuthAccountGenerator} 生成：用户名 6 位大写字母数字、密码 8 位字母数字组合。
 * 账号生成后通过审计日志（device_auth_log）全程追溯查看/重置/启停操作。
 *
 * @author zwei
 */
@Service
@Slf4j
public class DeviceServiceImpl implements IDeviceService {
    private static final String REGISTER_SOURCE_MANUAL = "MANUAL";
    private static final int AUTH_STATUS_ENABLED = 1;
    private static final int AUTH_STATUS_DISABLED = 2;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final IDeviceHazardRelationService hazardRelationService;
    private final DeviceAuthAccountGenerator accountGenerator;
    private final DeviceAuthLogService deviceAuthLogService;
    private final ObjectProvider<IDeviceSessionService> deviceSessionServiceProvider;
    private final IDeviceStatusLogService deviceStatusLogService;
    private final IProductTslService productTslService;
    private final ProductMapper productMapper;

    @Autowired
    public DeviceServiceImpl(DeviceMapper deviceMapper, DeviceSensorMapper sensorMapper,
                             SensorAttributeMapper attributeMapper,
                             IDeviceHazardRelationService hazardRelationService,
                             DeviceAuthAccountGenerator accountGenerator,
                             DeviceAuthLogService deviceAuthLogService,
                             ObjectProvider<IDeviceSessionService> deviceSessionServiceProvider,
                             IDeviceStatusLogService deviceStatusLogService,
                             IProductTslService productTslService,
                             ProductMapper productMapper) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.hazardRelationService = hazardRelationService;
        this.accountGenerator = accountGenerator;
        this.deviceAuthLogService = deviceAuthLogService;
        this.deviceSessionServiceProvider = deviceSessionServiceProvider;
        this.deviceStatusLogService = deviceStatusLogService;
        this.productTslService = productTslService;
        this.productMapper = productMapper;
    }

    /**
     * 分页查询设备列表
     */
    @Override
    public List<Device> selectDevicePage(Device device, int pageNum, int pageSize) {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 查询所有设备列表
     */
    @Override
    public List<Device> selectDeviceAll() {
        return deviceMapper.selectDeviceAll();
    }

    /**
     * 根据ID查询设备详情
     */
    @Override
    public Device selectDeviceById(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device != null) {
            device.setSensors(loadDeviceSensors(id));
        }
        return device;
    }

    @Override
    @Transactional
    public Device createDevice(DeviceCreateRequest request, String operator) {
        Device device = new Device();
        device.setCode(normalizeRequired(request.getCode(), "设备编号不能为空"));
        device.setName(normalizeRequired(request.getName(), "设备名称不能为空"));
        device.setSn(normalizeOptional(request.getSn()));
        device.setDeviceType(request.getDeviceType());
        device.setNetworkType(request.getNetworkType());
        device.setProtocolType(normalizeProtocol(request.getProtocolType()));
        device.setRegisterSource(REGISTER_SOURCE_MANUAL);
        device.setVendorName(normalizeOptional(request.getVendorName()));
        device.setIcon(normalizeNullable(request.getIcon()));
        device.setIconPath(normalizeNullable(request.getIconPath()));
        device.setStatus(request.getStatus());
        device.setLongitude(request.getLongitude());
        device.setLatitude(request.getLatitude());
        device.setAuthUsername(accountGenerator.generateUsername());
        device.setAuthPassword(accountGenerator.generatePassword());
        device.setAuthStatus(AUTH_STATUS_ENABLED);
        device.setRegisteredAt(nowString());
        device.setCreateBy(operator);

        if (!checkDeviceCodeUnique(device)) {
            throw new ServiceException("新增失败，设备编码已存在");
        }
        validateSnUnique(device.getSn(), null);
        insertDevice(device);
        return deviceMapper.selectDeviceById(device.getId());
    }

    @Override
    @Transactional
    public Device updateDevice(Long id, DeviceUpdateRequest request, String operator) {
        Device current = requireDevice(id);
        Device device = new Device();
        device.setId(id);
        device.setName(normalizeRequired(request.getName(), "设备名称不能为空"));
        device.setSn(normalizeOptional(request.getSn()));
        device.setDeviceType(request.getDeviceType());
        device.setNetworkType(request.getNetworkType());
        device.setProtocolType(normalizeProtocol(request.getProtocolType()));
        device.setVendorName(normalizeOptional(request.getVendorName()));
        device.setIcon(normalizeNullable(request.getIcon()));
        device.setIconPath(normalizeNullable(request.getIconPath()));
        device.setStatus(request.getStatus());
        device.setLongitude(request.getLongitude());
        device.setLatitude(request.getLatitude());
        device.setUpdateBy(operator);

        validateSnUnique(device.getSn(), id);
        deviceMapper.updateDevice(device);

        Device latest = deviceMapper.selectDeviceById(id);
        if (latest != null) {
            latest.setSensors(loadDeviceSensors(id));
            latest.setCode(current.getCode());
        }
        return latest;
    }

    /**
     * 新增设备
     */
    @Override
    public int insertDevice(Device device) {
        return deviceMapper.insertDevice(device);
    }

    /**
     * 修改设备
     */
    @Override
    public int updateDevice(Device device) {
        return deviceMapper.updateDevice(device);
    }

    /**
     * 删除设备（逻辑删除）
     */
    @Override
    public int deleteDeviceById(Long id) {
        List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(List.of(id));
        deleteSensorAttributesByDeviceId(id);
        sensorMapper.deleteSensorByDeviceId(id);
        hazardRelationService.deleteBindingsByDeviceIds(List.of(id));
        productMapper.deleteByDeviceId(id);
        int rows = deviceMapper.deleteDeviceById(id);
        refreshHazardPointDeviceCounts(hazardPointIds);
        return rows;
    }

    /**
     * 批量删除设备（逻辑删除）
     */
    @Override
    public int deleteDeviceByIds(Long[] ids) {
        List<Long> deviceIds = new ArrayList<>(List.of(ids));
        List<Long> hazardPointIds = hazardRelationService.getHazardPointIdsByDeviceIds(deviceIds);
        for (Long id : ids) {
            deleteSensorAttributesByDeviceId(id);
            sensorMapper.deleteSensorByDeviceId(id);
            productMapper.deleteByDeviceId(id);
        }
        hazardRelationService.deleteBindingsByDeviceIds(deviceIds);
        int rows = deviceMapper.deleteDeviceByIds(ids);
        refreshHazardPointDeviceCounts(hazardPointIds);
        return rows;
    }

    /**
     * 复制设备
     */
    @Override
    @Transactional
    public Long copyDevice(Long id) {
        Device original = requireDevice(id);
        Device copy = Device.builder()
                .code(original.getCode() + "_copy")
                .name(original.getName() + "_副本")
                .sn(null)
                .deviceType(original.getDeviceType())
                .networkType(original.getNetworkType())
                .protocolType(original.getProtocolType())
                .registerSource(REGISTER_SOURCE_MANUAL)
                .vendorName(original.getVendorName())
                .authUsername(accountGenerator.generateUsername())
                .authPassword(accountGenerator.generatePassword())
                .authStatus(AUTH_STATUS_ENABLED)
                .icon(original.getIcon())
                .iconPath(original.getIconPath())
                .status(original.getStatus())
                .registeredAt(nowString())
                .createBy(original.getCreateBy())
                .build();

        deviceMapper.insertDevice(copy);

        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(id);
        for (DeviceSensor originalSensor : sensors) {
            DeviceSensor newSensor = DeviceSensor.builder()
                    .deviceId(copy.getId())
                    .deviceCode(copy.getCode())
                    .sensorCode(originalSensor.getSensorCode() + "_copy")
                    .sensorName(originalSensor.getSensorName())
                    .monitorTypeId(originalSensor.getMonitorTypeId())
                    .monitorTypeCode(originalSensor.getMonitorTypeCode())
                    .monitorTypeName(originalSensor.getMonitorTypeName())
                    .status(originalSensor.getStatus())
                    .createBy(original.getCreateBy())
                    .build();
            sensorMapper.insertSensor(newSensor);

            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(originalSensor.getId());
            if (!attrs.isEmpty()) {
                for (SensorAttribute attr : attrs) {
                    attr.setId(null);
                    attr.setSensorId(newSensor.getId());
                    attr.setCreateBy(original.getCreateBy());
                }
                attributeMapper.batchInsertAttribute(attrs);
            }
        }
        productTslService.regenerate(copy.getId());
        return copy.getId();
    }

    /**
     * 校验设备编码是否唯一
     */
    @Override
    public boolean checkDeviceCodeUnique(Device device) {
        Device result = deviceMapper.checkDeviceCodeUnique(device.getCode(), device.getId());
        return result == null;
    }

    /**
     * 获取设备传感器列表
     */
    @Override
    public List<DeviceSensor> selectSensorListByDeviceId(Long deviceId) {
        return loadDeviceSensors(deviceId);
    }

    @Override
    public Device getDeviceAuthAccount(Long deviceId, String operator, String clientIp) {
        Device device = requireDevice(deviceId);
        saveAuthAuditLog(device, operator, clientIp, true, "WEB_VIEW_ACCOUNT");
        return device;
    }

    @Override
    @Transactional
    public Device resetDeviceAuthPassword(Long deviceId, String operator, String resetReason, Boolean forceOffline, String clientIp) {
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

    @Override
    @Transactional
    public Device changeDeviceAuthStatus(Long deviceId, Integer authStatus, String operator, String reason, String clientIp) {
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

    private void deleteSensorAttributesByDeviceId(Long deviceId) {
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(deviceId);
        for (DeviceSensor sensor : sensors) {
            attributeMapper.deleteAttributeBySensorId(sensor.getId());
        }
    }

    private void refreshHazardPointDeviceCounts(List<Long> hazardPointIds) {
        if (hazardPointIds == null || hazardPointIds.isEmpty()) {
            return;
        }
        for (Long hazardPointId : hazardPointIds) {
            hazardRelationService.refreshDeviceCount(hazardPointId);
        }
    }

    private Device requireDevice(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device == null) {
            throw new ServiceException("设备不存在");
        }
        return device;
    }

    private List<DeviceSensor> loadDeviceSensors(Long deviceId) {
        List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(deviceId);
        for (DeviceSensor sensor : sensors) {
            List<SensorAttribute> attrs = attributeMapper.selectAttributeListBySensorId(sensor.getId());
            sensor.setAttrList(attrs);
        }
        return sensors;
    }

    private void validateSnUnique(String sn, Long excludeId) {
        if (sn == null || sn.isBlank()) {
            return;
        }
        Device existing = deviceMapper.selectDeviceBySn(sn);
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new ServiceException("设备SN已存在");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeProtocol(String protocolType) {
        String normalized = normalizeOptional(protocolType);
        return normalized == null ? "MQTT" : normalized.toUpperCase();
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

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Override
    @Transactional
    public String maintenanceDevice(Long deviceId, Integer operationType, String operatorName, String operatorPhone,
                                    String operationDate, String description, String createBy) {
        Device device = requireDevice(deviceId);
        int oldStatus = device.getStatus() != null ? device.getStatus() : 1;

        // ── 状态转换校验 ──
        int newStatus = resolveAndValidateStatusTransition(operationType, oldStatus);

        String statusText = switch (operationType) {
            case 1 -> "报修";
            case 2 -> "修复";
            case 3 -> "停用";
            case 4 -> "启用";
            default -> throw new ServiceException("不支持的操作类型: " + operationType);
        };

        // ── 更新设备状态 ──
        Device update = new Device();
        update.setId(deviceId);
        update.setStatus(newStatus);
        update.setUpdateBy(createBy);
        deviceMapper.updateDevice(update);

        // ── 写入运维日志 ──
        deviceStatusLogService.saveMaintenanceLog(deviceId, device.getCode(), oldStatus, newStatus,
                statusText, operatorName, operatorPhone, operationDate, description, createBy);

        log.info("设备维修操作完成 deviceId={}, operation={}, {}→{}, operator={}",
                deviceId, statusText, oldStatus, newStatus, createBy);
        return statusText;
    }

    /**
     * 解析操作类型并校验状态转换合法性。
     */
    private int resolveAndValidateStatusTransition(int operationType, int oldStatus) {
        int newStatus = switch (operationType) {
            case 1 -> { // 报修：仅允许从 正常(1) 转入 维修(2)
                if (oldStatus != 1) throw new ServiceException("仅正常状态的设备可以报修");
                yield 2;
            }
            case 2 -> { // 修复：仅允许从 维修(2) 转入 正常(1)
                if (oldStatus != 2) throw new ServiceException("仅维修状态的设备可以修复");
                yield 1;
            }
            case 3 -> { // 停用：允许从 正常(1) 或 维修(2) 转入 停用(3)
                if (oldStatus != 1 && oldStatus != 2) throw new ServiceException("仅正常或维修状态的设备可以停用");
                yield 3;
            }
            case 4 -> { // 恢复：仅允许从 停用(3) 转入 正常(1)
                if (oldStatus != 3) throw new ServiceException("仅停用状态的设备可以恢复");
                yield 1;
            }
            default -> throw new ServiceException("不支持的操作类型: " + operationType);
        };
        return newStatus;
    }

    private String nowString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
