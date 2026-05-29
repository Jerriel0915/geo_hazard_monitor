package com.zwei.iot.device.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.device.domain.Device;
import com.zwei.iot.device.domain.DeviceRegistrationLog;
import com.zwei.iot.device.domain.DeviceSensor;
import com.zwei.iot.device.domain.SensorAttribute;
import com.zwei.iot.device.domain.dto.DeviceRegisterChildDeviceRequest;
import com.zwei.iot.device.domain.dto.DeviceRegisterMonitorTypeRequest;
import com.zwei.iot.device.domain.dto.DeviceRegisterRequest;
import com.zwei.iot.device.domain.vo.DeviceRegistryResult;
import com.zwei.iot.device.mapper.DeviceMapper;
import com.zwei.iot.device.mapper.DeviceRegistrationLogMapper;
import com.zwei.iot.device.mapper.DeviceSensorMapper;
import com.zwei.iot.device.mapper.SensorAttributeMapper;
import com.zwei.iot.device.service.DeviceRegistrationLogService;
import com.zwei.iot.device.service.IDeviceRegistryService;
import com.zwei.iot.device.support.DeviceAuthAccountGenerator;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 设备注册中心服务
 */
@Service
public class DeviceRegistryServiceImpl implements IDeviceRegistryService {
    private static final String REGISTER_SOURCE_API = "API";
    private static final String REGISTER_RESULT_SUCCESS = "SUCCESS";
    private static final String REGISTER_RESULT_FAIL = "FAIL";
    private static final int AUTH_STATUS_ENABLED = 1;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceMapper deviceMapper;
    private final DeviceSensorMapper sensorMapper;
    private final SensorAttributeMapper attributeMapper;
    private final DeviceRegistrationLogMapper registrationLogMapper;
    private final DeviceRegistrationLogService registrationLogService;
    private final IMonitorTypeService monitorTypeService;
    private final IMonitorContentService monitorContentService;
    private final DeviceAuthAccountGenerator accountGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Value("${zwei.iot.device-registry.register-codes:ABCDEF123456}")
    private String configuredRegisterCodes;

    public DeviceRegistryServiceImpl(DeviceMapper deviceMapper,
                                     DeviceSensorMapper sensorMapper,
                                     SensorAttributeMapper attributeMapper,
                                     DeviceRegistrationLogMapper registrationLogMapper,
                                     DeviceRegistrationLogService registrationLogService,
                                     IMonitorTypeService monitorTypeService,
                                     IMonitorContentService monitorContentService,
                                     DeviceAuthAccountGenerator accountGenerator) {
        this.deviceMapper = deviceMapper;
        this.sensorMapper = sensorMapper;
        this.attributeMapper = attributeMapper;
        this.registrationLogMapper = registrationLogMapper;
        this.registrationLogService = registrationLogService;
        this.monitorTypeService = monitorTypeService;
        this.monitorContentService = monitorContentService;
        this.accountGenerator = accountGenerator;
    }

    @Override
    @Transactional
    public DeviceRegistryResult register(DeviceRegisterRequest request) {
        String requestId = normalizeRequired(request.getRequestId(), "requestId不能为空");
        String payload = serializeRequest(request);

        DeviceRegistrationLog existingLog = registrationLogService.selectByRequestId(requestId);
        if (existingLog != null) {
            return handleExistingRequest(existingLog);
        }

        try {
            validateRegisterCode(request.getRegisterCode());
            Device existingDevice = deviceMapper.selectDeviceBySn(normalizeRequired(request.getSn(), "sn不能为空"));
            if (existingDevice != null) {
                ensureRequestConsistent(existingDevice, request);
                ensureDeviceAuthAccount(existingDevice);
                return new DeviceRegistryResult(deviceMapper.selectDeviceById(existingDevice.getId()), false);
            }

            Device device = buildDevice(request);
            deviceMapper.insertDevice(device);
            createSensors(device, request);
            registrationLogMapper.insert(buildLog(request, payload, device.getId(), REGISTER_RESULT_SUCCESS, null));
            return new DeviceRegistryResult(deviceMapper.selectDeviceById(device.getId()), true);
        } catch (RuntimeException ex) {
            registrationLogService.save(buildLog(request, payload, null, REGISTER_RESULT_FAIL, ex.getMessage()));
            throw ex;
        }
    }

    private DeviceRegistryResult handleExistingRequest(DeviceRegistrationLog existingLog) {
        if (REGISTER_RESULT_SUCCESS.equalsIgnoreCase(existingLog.getResultStatus()) && existingLog.getDeviceId() != null) {
            Device device = deviceMapper.selectDeviceById(existingLog.getDeviceId());
            if (device == null) {
                throw new ServiceException("注册记录存在，但设备不存在", 500);
            }
            return new DeviceRegistryResult(device, false);
        }
        throw new ServiceException(existingLog.getFailureReason() == null ? "该请求注册失败" : existingLog.getFailureReason(), 409);
    }

    private void validateRegisterCode(String registerCode) {
        String normalized = normalizeRequired(registerCode, "registerCode不能为空");
        for (String configuredCode : configuredRegisterCodes.split(",")) {
            if (normalized.equalsIgnoreCase(configuredCode.trim())) {
                return;
            }
        }
        throw new ServiceException("注册码无效", 401);
    }

    private Device buildDevice(DeviceRegisterRequest request) {
        Device device = new Device();
        String sn = normalizeRequired(request.getSn(), "sn不能为空");
        device.setCode(buildUniqueDeviceCode(sn));
        device.setName(normalizeRequired(request.getDeviceName(), "deviceName不能为空"));
        device.setSn(sn);
        device.setDeviceType(parseInteger(request.getDeviceType(), "deviceType非法"));
        device.setNetworkType(parseInteger(request.getNetwork(), "network非法"));
        device.setProtocolType(resolveProtocol(request.getProtocol()));
        device.setRegisterSource(REGISTER_SOURCE_API);
        device.setVendorName(normalizeOptional(request.getVendorName()));
        device.setStatus(1);
        device.setAuthUsername(accountGenerator.generateUsername());
        device.setAuthPassword(accountGenerator.generatePassword());
        device.setAuthStatus(AUTH_STATUS_ENABLED);
        device.setRegisteredAt(nowString());
        device.setCreateBy("device-registry");
        return device;
    }

    private void createSensors(Device device, DeviceRegisterRequest request) {
        Set<String> sensorNoSet = new HashSet<>();
        for (DeviceRegisterMonitorTypeRequest monitorType : request.getMonitorTypes()) {
            RegistrationSensorSpec spec = buildSensorSpec(
                    device,
                    normalizeRequired(request.getDeviceName(), "deviceName不能为空"),
                    null,
                    monitorType,
                    sensorNoSet
            );
            insertSensor(device, spec);
        }
        if (request.getChildDevices() == null) {
            return;
        }
        for (DeviceRegisterChildDeviceRequest childDevice : request.getChildDevices()) {
            for (DeviceRegisterMonitorTypeRequest monitorType : childDevice.getMonitorTypes()) {
                RegistrationSensorSpec spec = buildSensorSpec(
                        device,
                        normalizeRequired(childDevice.getDeviceName(), "子设备名称不能为空"),
                        normalizeRequired(childDevice.getSn(), "子设备SN不能为空"),
                        monitorType,
                        sensorNoSet
                );
                insertSensor(device, spec);
            }
        }
    }

    private RegistrationSensorSpec buildSensorSpec(Device device,
                                                   String deviceName,
                                                   String childSn,
                                                   DeviceRegisterMonitorTypeRequest monitorTypeRequest,
                                                   Set<String> sensorNoSet) {
        MonitorType monitorType = requireMonitorType(monitorTypeRequest.getType());
        List<MonitorContent> contents = monitorContentService.selectMonitorContentAll(monitorType.getId());
        String baseSensorNo = normalizeRequired(monitorTypeRequest.getSid(), "传感器编号不能为空");
        String sensorNo = childSn == null ? baseSensorNo : childSn + "_" + baseSensorNo;
        if (!sensorNoSet.add(sensorNo)) {
            throw new ServiceException("传感器编号重复: " + sensorNo, 400);
        }
        String sensorCode = buildUniqueSensorCode(device.getCode(), sensorNo);
        String sensorName = childSn == null
                ? deviceName + "-" + monitorType.getName()
                : deviceName + "-" + monitorType.getName() + "(" + childSn + ")";
        return new RegistrationSensorSpec(sensorCode, sensorNo, sensorName, monitorType, contents);
    }

    private void insertSensor(Device device, RegistrationSensorSpec spec) {
        DeviceSensor sensor = new DeviceSensor();
        sensor.setDeviceId(device.getId());
        sensor.setDeviceCode(device.getCode());
        sensor.setSensorCode(spec.sensorCode());
        sensor.setSensorNo(spec.sensorNo());
        sensor.setSensorName(spec.sensorName());
        sensor.setMonitorTypeId(spec.monitorType().getId());
        sensor.setMonitorTypeCode(spec.monitorType().getCode());
        sensor.setMonitorTypeName(spec.monitorType().getName());
        sensor.setStatus(1);
        sensor.setCreateBy("device-registry");
        sensorMapper.insertSensor(sensor);

        List<MonitorContent> contents = spec.contents();
        if (contents == null || contents.isEmpty()) {
            return;
        }
        for (MonitorContent content : contents) {
            SensorAttribute attribute = new SensorAttribute();
            attribute.setSensorId(sensor.getId());
            attribute.setAttrCode(content.getCode());
            attribute.setAttrName(content.getName());
            attribute.setIndicatorType(content.getIndicatorType());
            attribute.setIndicatorTypeName(resolveIndicatorTypeName(content.getIndicatorType()));
            attribute.setInitialValue(BigDecimal.ZERO);
            attribute.setUnit(content.getUnit());
            attribute.setRangeMin(content.getRangeMin());
            attribute.setRangeMax(content.getRangeMax());
            attribute.setIcon(content.getIcon());
            attribute.setCreateBy("device-registry");
            attributeMapper.insertAttribute(attribute);
        }
    }

    private MonitorType requireMonitorType(String monitorTypeCode) {
        String code = normalizeRequired(monitorTypeCode, "监测类型编码不能为空");
        MonitorType monitorType = monitorTypeService.selectMonitorTypeByCode(code);
        if (monitorType == null) {
            throw new ServiceException("监测类型不存在: " + code, 404);
        }
        return monitorType;
    }

    private void ensureRequestConsistent(Device device, DeviceRegisterRequest request) {
        Integer requestDeviceType = parseInteger(request.getDeviceType(), "deviceType非法");
        Integer requestNetworkType = parseInteger(request.getNetwork(), "network非法");
        String requestProtocol = resolveProtocol(request.getProtocol());
        boolean consistent = Objects.equals(normalizeOptional(device.getName()), normalizeRequired(request.getDeviceName(), "deviceName不能为空"))
                && Objects.equals(device.getDeviceType(), requestDeviceType)
                && Objects.equals(device.getNetworkType(), requestNetworkType)
                && Objects.equals(normalizeOptional(device.getProtocolType()), requestProtocol);
        if (!consistent) {
            throw new ServiceException("设备SN已存在但请求信息不一致", 409);
        }
    }

    private void ensureDeviceAuthAccount(Device device) {
        boolean needUpdate = false;
        Device update = new Device();
        update.setId(device.getId());
        if (device.getAuthUsername() == null || device.getAuthUsername().isBlank()) {
            update.setAuthUsername(accountGenerator.generateUsername());
            needUpdate = true;
        }
        if (device.getAuthPassword() == null || device.getAuthPassword().isBlank()) {
            update.setAuthPassword(accountGenerator.generatePassword());
            needUpdate = true;
        }
        if (device.getAuthStatus() == null) {
            update.setAuthStatus(AUTH_STATUS_ENABLED);
            needUpdate = true;
        }
        if (!needUpdate) {
            return;
        }
        update.setUpdateBy("device-registry");
        deviceMapper.updateDevice(update);
    }

    private DeviceRegistrationLog buildLog(DeviceRegisterRequest request,
                                           String payload,
                                           Long deviceId,
                                           String resultStatus,
                                           String failureReason) {
        DeviceRegistrationLog log = new DeviceRegistrationLog();
        log.setRequestId(normalizeRequired(request.getRequestId(), "requestId不能为空"));
        log.setRegisterCode(normalizeRequired(request.getRegisterCode(), "registerCode不能为空"));
        log.setRegisterSource(REGISTER_SOURCE_API);
        log.setVendorName(normalizeOptional(request.getVendorName()));
        log.setDeviceId(deviceId);
        log.setSn(normalizeRequired(request.getSn(), "sn不能为空"));
        log.setResultStatus(resultStatus);
        log.setFailureReason(failureReason);
        log.setRequestBody(payload);
        return log;
    }

    private String buildUniqueDeviceCode(String sn) {
        String base = "DEV-" + sn;
        String candidate = base;
        int index = 1;
        while (deviceMapper.selectDeviceByCode(candidate) != null) {
            candidate = base + "-" + index++;
        }
        return candidate;
    }

    private String buildUniqueSensorCode(String deviceCode, String sensorNo) {
        String normalizedSensorNo = sensorNo.replaceAll("[^A-Za-z0-9_-]", "_");
        String base = deviceCode + "_" + normalizedSensorNo;
        String candidate = base;
        int index = 1;
        while (sensorMapper.selectSensorByCode(candidate) != null) {
            candidate = base + "_" + index++;
        }
        return candidate;
    }

    private Integer parseInteger(String value, String message) {
        try {
            return Integer.parseInt(normalizeRequired(value, message));
        } catch (NumberFormatException ex) {
            throw new ServiceException(message, 400);
        }
    }

    private String resolveProtocol(String protocol) {
        String normalized = normalizeRequired(protocol, "protocol不能为空");
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "0", "MQTT" -> "MQTT";
            case "1", "HTTP" -> "HTTP";
            case "2", "COAP" -> "COAP";
            default -> throw new ServiceException("protocol非法", 400);
        };
    }

    private String resolveIndicatorTypeName(String indicatorType) {
        if (indicatorType == null) {
            return null;
        }
        return switch (indicatorType) {
            case "wy" -> "位移";
            case "wd" -> "温度";
            case "jd" -> "角度";
            case "yl" -> "压力";
            case "sw" -> "水位";
            case "jsd" -> "加速度";
            case "hsl" -> "含水率";
            case "ljn" -> "力矩";
            case "zdl" -> "震动频率";
            case "dl" -> "电量";
            case "dx" -> "断线";
            case "sg" -> "声光";
            case "sp" -> "视频";
            default -> indicatorType;
        };
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ServiceException(message, 400);
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

    private String serializeRequest(DeviceRegisterRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            List<String> parts = new ArrayList<>();
            parts.add("requestId=" + request.getRequestId());
            parts.add("sn=" + request.getSn());
            parts.add("deviceName=" + request.getDeviceName());
            return String.join(",", parts);
        }
    }

    private String nowString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    private record RegistrationSensorSpec(String sensorCode,
                                          String sensorNo,
                                          String sensorName,
                                          MonitorType monitorType,
                                          List<MonitorContent> contents) {
    }
}
