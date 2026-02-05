package com.zwei.iot.storage.mysql.service;

import com.zwei.iot.core.thing.domain.ThingModel;
import com.zwei.iot.core.thing.domain.TslEvent;
import com.zwei.iot.core.thing.domain.TslProperty;
import com.zwei.iot.storage.core.IDbStructureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL数据源的物模型定义实现
 * 按照物模型属性创建产品表，属性作为表字段，表名为zw_ts_{产品key}
 *
 * @author linx
 */
@Component
@Primary
public class DbStructureDataImpl implements IDbStructureData {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(DbStructureDataImpl.class);

    /**
     * 构造函数注入JdbcTemplate
     */
    @Autowired
    public DbStructureDataImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void defineThingModel(ThingModel thingModel) {
        if (thingModel == null || thingModel.getProfile() == null) {
            return;
        }

        // 按照要求使用新的表名规则：zw_ts_{产品key}
        String tableName = "zw_ts_" + thingModel.getProfile().getProductKey();

        // 根据物模型属性创建产品表，属性作为表字段
        createProductTable(tableName, thingModel.getProperties());

        // 创建事件表（保持事件表功能）
        createEventTable(tableName + "_event", thingModel.getEvents());
    }

    @Override
    public void updateThingModel(ThingModel thingModel) {
        if (thingModel == null || thingModel.getProfile() == null) {
            return;
        }

        // 按照要求使用新的表名规则
        String tableName = "zw_ts_" + thingModel.getProfile().getProductKey();

        // 检查表是否存在
        if (tableExists(tableName)) {
            // 更新表结构，根据新的物模型属性添加或修改字段
            updateProductTable(tableName, thingModel.getProperties());
        } else {
            // 如果表不存在，创建新表
            defineThingModel(thingModel);
        }
    }

    @Override
    public void initDbStructure() {
        // 初始化基础表结构
        String createDeviceStatusTable = "CREATE TABLE IF NOT EXISTS zw_iot_device_status (" +
                "    device_id VARCHAR(128) NOT NULL, " +
                "    status INT DEFAULT 0, " +
                "    last_report_time BIGINT, " +
                "    last_connect_time BIGINT, " +
                "    last_offline_time BIGINT, " +
                "    PRIMARY KEY (device_id) " +
                ") COMMENT '设备实时状态表';";

        jdbcTemplate.execute(createDeviceStatusTable);

        // 可以在这里初始化其他必要的基础表
    }

    /**
     * 创建产品表，将物模型属性作为表字段
     */
    private void createProductTable(String tableName, List<TslProperty> properties) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" ( ")
                .append("id BIGINT AUTO_INCREMENT PRIMARY KEY, ")
                .append("device_id VARCHAR(128) NOT NULL, ") // 统一添加设备ID字段
                .append("report_time BIGINT NOT NULL, ") // 时间戳字段
                .append("create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "); // 创建时间

        // 添加物模型属性作为表字段
        if (properties != null && !properties.isEmpty()) {
            for (TslProperty property : properties) {
                String fieldName = property.getIdentifier();
                String fieldType = getMysqlDataType(property.getDataType().getType().getCode());
                sql.append(fieldName).append(" ").append(fieldType).append(" COMMENT '").append(property.getName())
                        .append("', ");
            }
        }

        // 添加索引
        sql.append("INDEX idx_device_id (device_id), ")
                .append("INDEX idx_report_time (report_time) ")
                .append(") COMMENT '产品数据表 - ").append(tableName).append("';");

        jdbcTemplate.execute(sql.toString());
    }

    /**
     * 创建事件表
     */
    private void createEventTable(String tableName, List<TslEvent> events) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ( " +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "device_id VARCHAR(128) NOT NULL, " +
                "event_name VARCHAR(128) NOT NULL, " +
                "event_data TEXT, " +
                "event_time BIGINT NOT NULL, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "INDEX idx_device_id (device_id), " +
                "INDEX idx_event_name (event_name), " +
                "INDEX idx_event_time (event_time) " +
                ") COMMENT '设备事件数据表';";

        jdbcTemplate.execute(sql);
    }

    /**
     * 更新产品表结构
     */
    private void updateProductTable(String tableName, List<TslProperty> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        // 获取现有表的所有列名
        List<String> existingColumns = getTableColumns(tableName);

        // 为每个属性添加列（如果不存在）
        for (TslProperty property : properties) {
            String fieldName = property.getIdentifier();

            // 跳过已经存在的列
            if (!existingColumns.contains(fieldName)) {
                String fieldType = getMysqlDataType(property.getDataType().getType().getCode());
                String addColumnSql = "ALTER TABLE " + tableName + " ADD COLUMN " +
                        fieldName + " " + fieldType + " COMMENT '" +
                        property.getName() + "';";
                jdbcTemplate.execute(addColumnSql);
            }
        }

        System.out.println("Updated product table: " + tableName + ", properties: " +
                properties.stream().map(TslProperty::getIdentifier).collect(Collectors.joining(", ")));
    }

    /**
     * 获取表的所有列名
     */
    private List<String> getTableColumns(String tableName) {
        try {
            String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_NAME = ?";
            return jdbcTemplate.queryForList(sql, new Object[] { tableName }, String.class);
        } catch (Exception e) {
            log.error("Error while fetching columns for table {}: {}", tableName, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 根据物模型属性类型获取对应的MySQL数据类型
     */
    private String getMysqlDataType(String propertyType) {
        if (propertyType == null) {
            return "VARCHAR(255)";
        }

        switch (propertyType.toLowerCase()) {
            case "string":
            case "text":
                return "TEXT";
            case "int":
            case "integer":
                return "INT";
            case "long":
                return "BIGINT";
            case "float":
                return "FLOAT";
            case "double":
                return "DOUBLE";
            case "boolean":
            case "bool":
                return "TINYINT(1)";
            case "datetime":
            case "date":
                return "DATETIME";
            case "timestamp":
                return "BIGINT";
            default:
                return "VARCHAR(255)";
        }
    }

    /**
     * 检查表是否存在
     */
    private boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, new Object[] { tableName }, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}