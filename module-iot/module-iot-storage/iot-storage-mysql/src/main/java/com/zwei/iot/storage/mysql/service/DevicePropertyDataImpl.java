package com.zwei.iot.storage.mysql.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.iot.core.thing.domain.DeviceProperty;
import com.zwei.iot.core.thing.domain.DevicePropertyCache;
import com.zwei.iot.storage.core.IDevicePropertyData;
import com.zwei.iot.storage.mysql.TableMetaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MySQL设备属性数据实现
 *
 * @author linx
 */
@Component
@Slf4j
@Primary
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

    /**
     * 设备属性建表, 格式为 zw_iot_device_property_{productKey}
     *
     * @param productKey
     */
    public void createDeviceProperty(String productKey) {
        try {
            // 构建表名
            String tableName = "zw_iot_device_property_" + productKey;

            // 检查表名合法性
            if (productKey == null || productKey.isEmpty()) {
                throw new InvalidParameterException("productKey is null!");
            }

            // 防止 sql 注入
            if (!productKey.matches("^[a-zA-Z0-9_-]+$")) {
                throw new InvalidParameterException("productKey format invalid!");
            }

            String sql_1 = "SELECT COUNT(id) FROM zw_iot_product WHERE product_key = ?";
            String row = jdbcTemplate.queryForObject(sql_1, new Object[]{productKey}, String.class);
            if (row.isEmpty() || row.equals("0")) {
                throw new InvalidParameterException("productKey is not found!");
            }

            // 建表
            String sql_2 = "CREATE TABLE IF NOT EXISTS " + tableName + " ( " +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "device_id VARCHAR(128) NOT NULL, " +
                    "property_name VARCHAR(128) NOT NULL, " +
                    "property_value VARCHAR(128) NOT NULL, " +
                    "report_time BIGINT NOT NULL);";

            jdbcTemplate.execute(sql_2);
            log.debug("成功创建了 {} 表", tableName);
        } catch (Exception e) {
            log.error("创建设备属性表失败: {}, productKey: {}", e.getMessage(), productKey);
        }
    }

    @Override
    public List<DeviceProperty> findDevicePropertyHistory(String deviceId, String name, long start, long end, int size) {
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
                    deviceId, name, start, end, size);

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
            log.error("查询设备属性历史数据失败: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public void addProperties(String deviceId, Map<String, DevicePropertyCache> properties, long time) {
        if (deviceId == null || properties == null || properties.isEmpty()) {
            return;
        }

        try {
            // 获取产品key
            String productKey = getProductKeyFromDeviceId(deviceId);
            String tableName = "zw_iot_device_property_" + productKey;

            if (productKey == null || productKey.isEmpty()) {
                throw new InvalidParameterException("productKey is null!");
            }

            // 表不存在则建表
            if (!TableMetaUtils.tableExists(jdbcTemplate, tableName)) {
                createDeviceProperty(productKey);
            }

            // 批量插入属性数据
            String sql = "INSERT INTO " + tableName + " (device_id, property_name, property_value, report_time) VALUES (?, ?, ?, ?)";

            List<Object[]> batchArgs = new ArrayList<>();
            for (Map.Entry<String, DevicePropertyCache> entry : properties.entrySet()) {
                String propertyName = entry.getKey();
                DevicePropertyCache propertyCache = entry.getValue();

                // 将属性值转换为字符串存储
                String propertyValue = convertToString(propertyCache.getValue());

                batchArgs.add(new Object[]{deviceId, propertyName, propertyValue, time});
            }

            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
            }

        } catch (Exception e) {
            // 记录异常但不抛出，避免影响正常流程
            log.error("添加设备属性数据失败, deviceId: {}, properties: {}, time: {}", deviceId, properties, time, e);
        } finally {
            // 无论属性是否上报成功，均更新最后上报时间
            updateDeviceLastReportTime(deviceId, time);
        }
    }

    /**
     * 从设备ID中提取产品Key
     * 注意：这里需要根据实际的设备ID生成规则来实现
     */
    private String getProductKeyFromDeviceId(String deviceId) {
        // 这里简化处理，实际应该从设备ID或设备表中获取对应的产品Key
        // 可以通过查询数据库或缓存来获取设备所属的产品Key


        String sql_1 = "SELECT product_id FROM zw_iot_device WHERE sn = ?";
        String productId = jdbcTemplate.queryForObject(sql_1, new Object[]{deviceId}, String.class);
        String sql_2 = "SELECT product_key FROM zw_iot_product WHERE id = ?";
        String productKey = jdbcTemplate.queryForObject(sql_2, new Object[]{productId}, String.class);

        return productKey;

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
            String sql1 = "SELECT id FROM zw_iot_device WHERE sn = ?";
            long id = jdbcTemplate.queryForObject(sql1, new Object[]{deviceId}, Long.class);
            String sql2 = "INSERT INTO zw_iot_device_status (device_id, last_report_time) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_report_time = ?";
            jdbcTemplate.update(sql2, id, time, time);
        } catch (Exception e) {
            log.error("更新设备最后上报时间失败: {}", e.getMessage());
        }
    }
}