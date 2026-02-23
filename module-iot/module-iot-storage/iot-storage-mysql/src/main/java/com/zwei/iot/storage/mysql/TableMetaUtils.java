package com.zwei.iot.storage.mysql;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 一些可公用的 MYSQL 数据库操作工具
 *
 * @Author: Jerriel
 * @CreateTime: 2026-02-06
 */
public class TableMetaUtils {
    // 禁止实例化
    private TableMetaUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取表的所有列名
     *
     * @param jdbcTemplate 数据源
     * @param tableName    表名
     * @return 返回指定表名的所有列名，传参为空时返回 null
     * @throws DataRetrievalFailureException 当无法正确地获取到表名时抛出
     */
    public static List<String> getTableColumns(JdbcTemplate jdbcTemplate, String tableName) {
        if (jdbcTemplate == null || tableName.isEmpty()) return null;
        try {
            String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_NAME = ?";
            return jdbcTemplate.queryForList(sql, new Object[]{tableName}, String.class);
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Failed to fetch columns for table: " + tableName, e);
        }
    }

    /**
     * 检查表是否存在
     *
     * @param jdbcTemplate 数据源
     * @param tableName    表名
     * @return 表存在时返回 true，其余情况均返回 false
     */
    public static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = ?";
            Integer count = jdbcTemplate.queryForObject(sql, new Object[]{tableName}, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
