package com.zwei.iot.parser.service;

import com.zwei.iot.device.domain.tsl.ProductTsl;
import com.zwei.iot.device.domain.SensorMetadata;
import com.zwei.iot.device.service.IDeviceSensorQueryService;
import com.zwei.iot.device.service.IProductTslService;
import com.zwei.iot.parser.domain.DataParseStrategy;
import com.zwei.iot.parser.mapper.DataParseStrategyDeviceMapper;
import com.zwei.iot.parser.mapper.DataParseStrategyMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Parse metadata service — sensor metadata + TSL model + strategy matching.
 *
 * Moved from zwei-iot-timeseries and extended with TSL query and 3-tier strategy resolution.
 */
@Service
public class MonitorMetadataService {

    @Resource
    private IDeviceSensorQueryService deviceSensorQueryService;
    @Resource
    private IProductTslService productTslService;
    @Resource
    private DataParseStrategyMapper strategyMapper;
    @Resource
    private DataParseStrategyDeviceMapper strategyDeviceMapper;

    /**
     * Get sensor metadata (preserves existing capability).
     */
    public SensorMetadata requireSensorMetadata(Long deviceId, String sensorCode) {
        return deviceSensorQueryService.requireSensorMetadata(deviceId, sensorCode);
    }

    /**
     * Get the device's TSL model (for value-range validation).
     */
    public ProductTsl getTsl(Long deviceId) {
        return productTslService.getByDeviceId(deviceId);
    }

    /**
     * 3-tier strategy matching.
     *
     * Priority: device-level → vendor-level (reserved) → global by sourceType.
     *
     * @param sourceType protocol identifier (sys/gb/custom)
     * @param deviceId   device primary key
     * @return matching strategy, or null if none found
     */
    public DataParseStrategy resolveStrategy(String sourceType, Long deviceId) {
        // Tier 1: device-level match
        Long deviceStrategyId = strategyDeviceMapper.selectStrategyIdByDeviceId(deviceId);
        if (deviceStrategyId != null) {
            DataParseStrategy strategy = strategyMapper.selectById(deviceStrategyId);
            if (strategy != null && strategy.getStatus() == 1) {
                return strategy;
            }
        }
        // Tier 2: vendor-level match (reserved for future, skip for now)
        // Tier 3: global match by sourceType
        List<DataParseStrategy> globalStrategies = strategyMapper.selectBySourceType(sourceType);
        if (globalStrategies != null && !globalStrategies.isEmpty()) {
            return globalStrategies.get(0);
        }
        return null;
    }
}
