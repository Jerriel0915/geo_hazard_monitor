package com.zwei.iot.timeseries.util;

import com.zwei.common.exception.ServiceException;
import com.zwei.common.utils.spring.SpringUtils;
import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.service.IotdbJdbcClient;
import com.zwei.iot.timeseries.support.IotdbPathResolver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 传感器数据查询静态工具类。
 *
 * <p>供非 Spring 管理的调用方（如 Groovy 告警脚本）查询传感器在某时刻的数据快照。
 * 内部通过 {@code SpringUtils.getBean} 获取 IoTDB 依赖，核心逻辑在包私有的 {@link #doQuery} 中，
 * 便于单元测试直接注入 mock。</p>
 */
public final class SensorDataQueryUtil {
    private SensorDataQueryUtil() {}

    /** attrCode 白名单：仅允许字母、数字、下划线，防 SQL 注入 */
    private static final Pattern ATTR_PATTERN = Pattern.compile("^[A-Za-z0-9_]+$");

    /**
     * 查询传感器在某时刻（或之前最近一条）的数据快照。
     *
     * <p>用法示例：
     * <pre>
     * // 查询设备 101 传感器 "WY_1" 在某时刻的全部属性
     * SensorSnapshot snap = SensorDataQueryUtil.query(101L, "WY_1", 1718294400000L, null);
     * if (snap != null) {
     *     long t = snap.getTime();
     *     Double rain = snap.getValues().get("rainfall_hour");
     * }
     * </pre>
     *
     * @param deviceId    设备ID
     * @param sensorCode  传感器编码
     * @param time        查询时刻（毫秒时间戳），返回 {@code time <= 此值} 的最近一条
     * @param attrCode    属性编码；为 {@code null}/空串时查询全部业务属性（排除 quality 列）
     * @return 数据快照；无数据时返回 {@code null}
     * @throws IllegalArgumentException 设备ID/传感器编码为空，或属性编码含非法字符
     */
    public static SensorSnapshot query(Long deviceId, String sensorCode, long time, String attrCode) {
        return doQuery(
                SpringUtils.getBean(IotdbJdbcClient.class),
                SpringUtils.getBean(IotdbPathResolver.class),
                deviceId, sensorCode, time, attrCode);
    }

    /**
     * 查询传感器在某时刻（或之前最近一条）的数据快照。包私有，便于单元测试直接注入依赖。
     *
     * @param jdbcClient  IoTDB JDBC 客户端
     * @param pathResolver IoTDB 路径解析器
     * @param deviceId    设备ID
     * @param sensorCode  传感器编码
     * @param time        查询时刻（毫秒时间戳）
     * @param attrCode    属性编码；为 {@code null}/空串时查询全部业务属性
     * @return 数据快照；无数据时返回 {@code null}
     */
    static SensorSnapshot doQuery(IotdbJdbcClient jdbcClient, IotdbPathResolver pathResolver,
                                   Long deviceId, String sensorCode, long time, String attrCode) {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId 不能为空");
        }
        if (sensorCode == null || sensorCode.isEmpty()) {
            throw new IllegalArgumentException("sensorCode 不能为空");
        }
        if (attrCode != null && !attrCode.isEmpty() && !ATTR_PATTERN.matcher(attrCode).matches()) {
            throw new IllegalArgumentException("attrCode 含非法字符: " + attrCode);
        }
        String sensorPath = pathResolver.buildSensorPath(deviceId, sensorCode);
        String selectClause = (attrCode == null || attrCode.isEmpty()) ? "*" : attrCode;
        String sql = "SELECT " + selectClause + " FROM " + sensorPath
                + " WHERE time <= " + time + " ORDER BY TIME DESC LIMIT 1";
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery(sql);
            if (!rs.next()) {
                return null;
            }
            long dataTime = rs.getLong("Time");
            Map<String, Double> values = new LinkedHashMap<>();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String col = md.getColumnLabel(i);
                if ("Time".equalsIgnoreCase(col)) {
                    continue;
                }
                String name = col.substring(col.lastIndexOf('.') + 1);
                if ("quality".equals(name)) {
                    continue;
                }
                Object v = rs.getObject(col);
                if (v != null) {
                    values.put(name, toDouble(v));
                }
            }
            return new SensorSnapshot(dataTime, values);
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 传感器数据失败").setDetailMessage(e.getMessage());
        }
    }

    /** 将 IoTDB 查询值转换为 Double；非数值返回 {@code null}。 */
    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
