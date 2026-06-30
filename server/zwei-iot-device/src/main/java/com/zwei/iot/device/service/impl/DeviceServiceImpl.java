package com.zwei.iot.device.service.impl;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.dto.DeviceCopyRequest;
import com.zwei.iot.device.domain.dto.DeviceCreateRequest;
import com.zwei.iot.device.domain.dto.DeviceUpdateRequest;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.ProductMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.*;
import com.zwei.iot.device.service.IDeviceHazardRelationService.HazardPointRef;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final IDeviceHazardRelationService hazardRelationService;
    private final DeviceAuthAccountGenerator accountGenerator;
    private final IProductTslService productTslService;
    private final ProductMapper productMapper;
    private final DeviceAuthService deviceAuthService;
    private final DeviceMaintenanceService deviceMaintenanceService;

    @Autowired
    public DeviceServiceImpl(DeviceMapper deviceMapper, DeviceSensorMapper sensorMapper,
                             SensorAttributeMapper attributeMapper,
                             IDeviceHazardRelationService hazardRelationService,
                             DeviceAuthAccountGenerator accountGenerator,
                             IProductTslService productTslService,
                             ProductMapper productMapper,
                             DeviceAuthService deviceAuthService,
                             DeviceMaintenanceService deviceMaintenanceService) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.hazardRelationService = hazardRelationService;
        this.accountGenerator = accountGenerator;
        this.productTslService = productTslService;
        this.productMapper = productMapper;
        this.deviceAuthService = deviceAuthService;
        this.deviceMaintenanceService = deviceMaintenanceService;
    }

    /**
     * 分页查询设备列表
     */
    @Override
    public List<Device> selectDevicePage(Device device, int pageNum, int pageSize) {
        List<Device> devices = deviceMapper.selectDeviceList(device);
        enrichHazardPoint(devices);
        return devices;
    }

    /**
     * 查询所有设备列表
     */
    @Override
    public List<Device> selectDeviceAll() {
        List<Device> devices = deviceMapper.selectDeviceAll();
        enrichHazardPoint(devices);
        return devices;
    }

    /**
     * 根据ID查询设备详情
     */
    @Override
    public Device selectDeviceById(Long id) {
        Device device = deviceMapper.selectDeviceById(id);
        if (device != null) {
            device.setSensors(loadDeviceSensors(id));
            enrichHazardPoint(device);
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

        if (request.getBoundHazardPointId() != null) {
            hazardRelationService.bindDevice(device.getId(), request.getBoundHazardPointId(),
                    request.getLongitude(), request.getLatitude(), operator);
        }

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

        // 处理隐患点绑定变更
        HazardPointRef oldBinding = hazardRelationService.getHazardPointByDeviceId(id);
        Long newHpId = request.getBoundHazardPointId();
        if (!Objects.equals(oldBinding != null ? oldBinding.id() : null, newHpId)) {
            if (oldBinding != null) {
                hazardRelationService.deleteBindingsByDeviceIds(List.of(id));
                hazardRelationService.refreshDeviceCount(oldBinding.id());
            }
            if (newHpId != null) {
                hazardRelationService.bindDevice(id, newHpId, request.getLongitude(), request.getLatitude(), operator);
            }
        }

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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
    public Long copyDevice(Long id, DeviceCopyRequest request) {
        Device original = requireDevice(id);

        // 校验新编号唯一性
        Device codeCheck = new Device();
        codeCheck.setCode(request.getCode());
        if (!checkDeviceCodeUnique(codeCheck)) {
            throw new ServiceException("复制失败，设备编号已存在");
        }

        validateSnUnique(request.getSn(), null);

        Device copy = Device.builder()
                .code(request.getCode())
                .name(request.getName())
                .sn(request.getSn())
                .deviceType(request.getDeviceType() != null ? request.getDeviceType() : original.getDeviceType())
                .networkType(request.getNetworkType() != null ? request.getNetworkType() : original.getNetworkType())
                .protocolType(request.getProtocolType() != null ? request.getProtocolType() : original.getProtocolType())
                .registerSource(REGISTER_SOURCE_MANUAL)
                .vendorName(request.getVendorName() != null ? request.getVendorName() : original.getVendorName())
                .authUsername(accountGenerator.generateUsername())
                .authPassword(accountGenerator.generatePassword())
                .authStatus(AUTH_STATUS_ENABLED)
                .icon(request.getIcon() != null ? request.getIcon() : original.getIcon())
                .iconPath(request.getIconPath() != null ? request.getIconPath() : original.getIconPath())
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : original.getLongitude())
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : original.getLatitude())
                .status(request.getStatus() != null ? request.getStatus() : original.getStatus())
                .registeredAt(nowString())
                .createBy(original.getCreateBy())
                .build();

        deviceMapper.insertDevice(copy);

        // 复制传感器（含属性），copySensors=false 时跳过
        if (Boolean.TRUE.equals(request.getCopySensors())) {
            List<DeviceSensor> sensors = sensorMapper.selectSensorListByDeviceId(id);
            for (DeviceSensor originalSensor : sensors) {
                String newSensorCode = resolveCopySensorCode(copy.getId(), originalSensor.getSensorCode());
                DeviceSensor newSensor = DeviceSensor.builder()
                        .deviceId(copy.getId())
                        .deviceCode(copy.getCode())
                        .sensorCode(newSensorCode)
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
        }

        productTslService.regenerate(copy.getId());

        // 复制隐患点绑定：优先使用用户指定的隐患点，否则沿用源设备的绑定
        if (request.getBoundHazardPointId() != null) {
            hazardRelationService.bindDevice(copy.getId(), request.getBoundHazardPointId(),
                    copy.getLongitude(), copy.getLatitude(), original.getCreateBy());
        } else {
            HazardPointRef oldBinding = hazardRelationService.getHazardPointByDeviceId(id);
            if (oldBinding != null) {
                hazardRelationService.bindDevice(copy.getId(), oldBinding.id(),
                        copy.getLongitude(), copy.getLatitude(), original.getCreateBy());
            }
        }

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

    // ── 认证账号管理 → 委托 DeviceAuthService ──

    @Override
    public Device getDeviceAuthAccount(Long deviceId, String operator, String clientIp) {
        return deviceAuthService.getDeviceAuthAccount(deviceId, operator, clientIp);
    }

    @Override
    @Transactional
    public Device resetDeviceAuthPassword(Long deviceId, String operator, String resetReason, Boolean forceOffline, String clientIp) {
        return deviceAuthService.resetDeviceAuthPassword(deviceId, operator, resetReason, forceOffline, clientIp);
    }

    @Override
    @Transactional
    public Device changeDeviceAuthStatus(Long deviceId, Integer authStatus, String operator, String reason, String clientIp) {
        return deviceAuthService.changeDeviceAuthStatus(deviceId, authStatus, operator, reason, clientIp);
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

    private static final int SENSOR_CODE_MAX_LEN = 100;
    private static final int MAX_SUFFIX_RETRY = 20;
    /** 匹配 _XX 后缀（2位数字），用于复制时自动递增 */
    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("^(.*)_(\\d{2})$");

    /**
     * 为复制的传感器生成设备内唯一编码。
     *
     * <h3>规则</h3>
     * <ol>
     *   <li>原编码末尾有 _XX（2位数字）→ 递增：sensor_03 → sensor_04</li>
     *   <li>原编码无此后缀 → 追加重置：sensor → sensor_01</li>
     *   <li>递增后冲突 → 从原编码重新追加重置：sensor → sensor_01_01</li>
     *   <li>仍冲突 → 逐步追加 _02、_03 直至唯一（上限 20 次）</li>
     *   <li>超过 varchar(100) 时截断原编码为后缀留空间</li>
     * </ol>
     *
     * @param deviceId 目标设备ID（复制目标设备，唯一性在该设备内校验）
     * @param originalCode 原传感器编码
     */
    private String resolveCopySensorCode(Long deviceId, String originalCode) {
        // Step 1: 尝试递增已有数字后缀
        String primary = incrementOrAppend(originalCode);
        String candidate = truncateToFit(primary, SENSOR_CODE_MAX_LEN);
        if (sensorMapper.checkSensorCodeUnique(deviceId, candidate, null) == null) {
            return candidate;
        }

        // Step 2: 冲突 — 从原编码重新追加 _01
        candidate = truncateToFit(originalCode + "_01", SENSOR_CODE_MAX_LEN);
        if (sensorMapper.checkSensorCodeUnique(deviceId, candidate, null) == null) {
            return candidate;
        }

        // Step 3: 逐步递增
        for (int i = 2; i <= MAX_SUFFIX_RETRY; i++) {
            String suffix = String.format("_%02d", i);
            candidate = truncateToFit(originalCode + suffix, SENSOR_CODE_MAX_LEN);
            if (sensorMapper.checkSensorCodeUnique(deviceId, candidate, null) == null) {
                return candidate;
            }
        }

        throw new ServiceException("复制传感器失败，无法生成唯一传感器编码");
    }

    /** 若末尾已有 _XX 则递增，否则追加 _01 */
    private static String incrementOrAppend(String code) {
        Matcher m = NUMERIC_SUFFIX.matcher(code);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(2)) + 1;
            return m.group(1) + String.format("_%02d", n);
        }
        return code + "_01";
    }

    /** 截断使总长 ≤ maxLen，优先保留右侧后缀 */
    private static String truncateToFit(String code, int maxLen) {
        return code.length() <= maxLen ? code : code.substring(code.length() - maxLen);
    }

    private void enrichHazardPoint(List<Device> devices) {
        if (devices == null || devices.isEmpty()) return;
        for (Device device : devices) {
            enrichHazardPoint(device);
        }
    }

    private void enrichHazardPoint(Device device) {
        HazardPointRef ref = hazardRelationService.getHazardPointByDeviceId(device.getId());
        if (ref != null) {
            device.setBoundHazardPointId(ref.id());
            device.setBoundHazardPointName(ref.name());
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

    // ── 设备维保状态机 → 委托 DeviceMaintenanceService ──

    @Override
    @Transactional
    public String maintenanceDevice(Long deviceId, Integer operationType, String operatorName, String operatorPhone,
                                    String operationDate, String description, String createBy) {
        return deviceMaintenanceService.maintenanceDevice(deviceId, operationType, operatorName, operatorPhone,
                operationDate, description, createBy);
    }

    private String nowString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
