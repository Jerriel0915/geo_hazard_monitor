package com.zwei.iot.storage.mysql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.iot.core.DeviceProperty;
import com.zwei.iot.core.DevicePropertyCache;
import com.zwei.iot.storage.core.IDevicePropertyData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQL设备属性数据实现
 *
 * @author linx
 */
@Component
public class DevicePropertyDataImpl implements IDevicePropertyData {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 构造函数注入JdbcTemplate
     */
    @Autowired
    public DevicePropertyDataImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DeviceProperty> findDevicePropertyHistory(Long deviceId, String name, long start, long end, int size) {
        List<DeviceProperty> result = new ArrayList<>();
        
        try {
            // 构建表名
            String tableName = "zw_iot_device_property_" + getProductKeyFromDeviceId(deviceId);
            
            // 查询历史数据
            String sql = "SELECT id, device_id, property_name, property_value, report_time " +
                    "FROM " + tableName + " " +
                    "WHERE device_id = ? AND property_name = ? AND report_time BETWEEN ? AND ? " +
                    "ORDER BY report_time DESC LIMIT ?";
            
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, 
                    deviceId.toString(), name, start, end, size);
            
            // 转换结果
            for (Map<String, Object> row : rows) {
                DeviceProperty property = new DeviceProperty();
                property.setId(row.get("id").toString());
                property.setDeviceId((String) row.get("device_id"));
                property.setName((String) row.get("property_name"));
                property.setValue(row.get("property_value"));
                property.setTime((Long) row.get("report_time"));
                result.add(property);
            }
        } catch (Exception e) {
            // 记录异常但不抛出，避免影响正常流程
            System.err.println("查询设备属性历史数据失败: " + e.getMessage());
        }
        
        return result;
    }

    @Override
    public void addProperties(Long deviceId, Map<String, DevicePropertyCache> properties, long time) {
        if (deviceId == null || properties == null || properties.isEmpty()) {
            return;
        }
        
        try {
            // 获取产品key
            String productKey = getProductKeyFromDeviceId(deviceId);
            String tableName = "zw_iot_device_property_" + productKey;
            
            // 批量插入属性数据
            String sql = "INSERT INTO " + tableName + " (device_id, property_name, property_value, report_time) VALUES (?, ?, ?, ?)";
            
            List<Object[]> batchArgs = new ArrayList<>();
            for (Map.Entry<String, DevicePropertyCache> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                DevicePropertyCache propertyCache = entry.getValue();
                
                // 将属性值转换为字符串存储
                String propertyValue = convertToString(propertyCache.getValue());
                
                batchArgs.add(new Object[]{deviceId.toString(), propertyName, propertyValue, time});
            }
            
            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }
            
            // 更新设备状态表中的最后上报时间
            updateDeviceLastReportTime(deviceId.toString(), time);
        } catch (Exception e) {
            // 记录异常但不抛出，避免影响正常流程
            System.err.println("添加设备属性数据失败: " + e.getMessage());
        }
    }

    /**
     * 从设备ID中提取产品Key
     * 注意：这里需要根据实际的设备ID生成规则来实现
     * 暂时返回一个默认值，实际应用中需要修改
     */
    private String getProductKeyFromDeviceId(Long deviceId) {
        // 这里简化处理，实际应该从设备ID或设备表中获取对应的产品Key
        // 可以通过查询数据库或缓存来获取设备所属的产品Key
        return "default_product";
    }

    /**
     * 将属性值转换为字符串
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        
        try {
            if (value instanceof String) {
                return (String) value;
            }
            // 使用Jackson将对象转换为JSON字符串
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            // 如果转换失败，返回对象的toString()结果
            return value.toString();
        }
    }

    /**
     * 更新设备最后上报时间
     */
    private void updateDeviceLastReportTime(String deviceId, long time) {
        try {
            String sql = "INSERT INTO zw_iot_device_status (device_id, last_report_time) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_report_time = ?";
            jdbcTemplate.update(sql, deviceId, time, time);
        } catch (Exception e) {
            System.err.println("更新设备最后上报时间失败: " + e.getMessage());
        }
    }
}