package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.IotdbQueryRow;
import com.zwei.iot.timeseries.domain.StandardMeasurementPoint;
import com.zwei.iot.timeseries.domain.ValueType;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * IoTDB 时序数据读写服务 — 动态建模、写入、查询。
 *
 * <h3>核心设计</h3>
 * <ol>
 *   <li><b>惰性建模</b>：首次写入/查询某个测点时自动创建 aligned timeseries，无需预先 DDL。
 *      使用 {@code ConcurrentHashMap} 缓存已创建的 measurement 路径，避免重复建表</li>
 *   <li><b>路径模型</b>：{@code root.{database}.d{deviceId}.s{sensorCode}.{attrCode}}</li>
 *   <li><b>Aligned Timeseries</b>：每个传感器路径下包含业务指标列（DOUBLE+GORILLA）和质量码列（INT32+RLE），
 *       同一时间戳的多个指标共用一个时间戳存储，节省空间</li>
 *   <li><b>建库幂等</b>：应用启动后首个写入触发 {@code CREATE DATABASE IF NOT EXISTS}，
 *      通过 {@code volatile databaseReady} 双重检查避免重复建库</li>
 * </ol>
 *
 * <h3>查询接口</h3>
 * <ul>
 *   <li>{@code queryLatest} — 最新值</li>
 *   <li>{@code queryRange} — 区间时序（支持 IoTDB 聚合函数 + 降采样间隔）</li>
 *   <li>{@code queryRangePaged} — 分页时序（LIMIT/OFFSET）</li>
 *   <li>{@code countRange} — 区间内数据条数</li>
 * </ul>
 *
 * @see IotdbPathResolver 路径解析
 * @see IotdbJdbcClient JDBC 连接管理
 */
@Slf4j
@Service
public class IotdbTimeSeriesService {
    private final IotdbJdbcClient jdbcClient;
    private final IotdbProperties properties;
    private final IotdbPathResolver pathResolver;
    private final ConcurrentMap<String, Boolean> createdMeasurements = new ConcurrentHashMap<>();
    private volatile boolean databaseReady;

    /**
     * 构造 IoTDB 时序服务。
     *
     * @param jdbcClient   IoTDB JDBC 客户端
     * @param properties   IoTDB 配置
     * @param pathResolver 路径解析器
     */
    @Autowired
    public IotdbTimeSeriesService(IotdbJdbcClient jdbcClient,
                                  IotdbProperties properties,
                                  IotdbPathResolver pathResolver) {
        this.jdbcClient = jdbcClient;
        this.properties = properties;
        this.pathResolver = pathResolver;
    }

    /**
     * 批量写入标准化时序点（使用 JDBC executeBatch 提升写入性能）。
     *
     * @param points 标准化时序点集合
     * @throws ServiceException 当 IoTDB 写入失败时抛出
     */
    public void writePoints(Collection<StandardMeasurementPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ensureDatabase();
        // 确保所有 measurement 已创建
        for (StandardMeasurementPoint point : points) {
            ensureMeasurement(point.attrCode(), point.deviceId(), point.sensorCode(), "DOUBLE", "GORILLA");
            ensureMeasurement("quality", point.deviceId(), point.sensorCode(), "INT32", "RLE");
        }
        // 批量组装 SQL → 单连接 executeBatch()
        List<String> sqlList = new ArrayList<>(points.size());
        for (StandardMeasurementPoint point : points) {
            sqlList.add("INSERT INTO " + pathResolver.buildSensorPath(point.deviceId(), point.sensorCode())
                    + "(timestamp," + point.attrCode() + ",quality) ALIGNED VALUES("
                    + point.dataTime() + "," + point.value() + "," + point.quality() + ")");
        }
        jdbcClient.executeBatch(sqlList);
    }

    /**
     * 为指定设备传感器预创建 IoTDB 时序 schema。
     * <p>
     * 在设备/传感器注册时调用，将建库建时序从写入热路径提前至注册冷路径，
     * 避免每条消息写入时触发 DDL 的 ERROR 日志。
     *
     * @param deviceId  设备ID
     * @param sensorCode  传感器编号
     * @param attrCodes 指标编码列表（不含 quality，quality 列自动创建）
     */
    public void createSensorSchema(Long deviceId, String sensorCode, List<String> attrCodes) {
        // 确保数据库表存在
        ensureDatabase();
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        for (String attrCode : attrCodes) {
            ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        }
    }

    /**
     * 查询指定测点指标的最新值。
     *
     * @param deviceId 设备ID
     * @param sensorCode 传感器编号
     * @param attrCode 指标编码
     * @return 最新查询结果；无数据时返回 {@code null}
     * @throws ServiceException 当查询失败时抛出
     */
    public IotdbQueryRow queryLatest(Long deviceId, String sensorCode, String attrCode) {
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        String sql = "SELECT " + attrCode + ", quality FROM " + pathResolver.buildSensorPath(deviceId, sensorCode)
                + " ORDER BY TIME DESC LIMIT 1";
        // IoTDB JDBC ResultSet 中列名为完整路径，需用 buildMeasurementPath 构造。
        // 同时移除 setFetchSize / setQueryTimeout（IoTDB JDBC 不支持）。
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, "quality");
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            if (!resultSet.next()) {
                return null;
            }
            Double value = safeGetDouble(resultSet, attrCol);
            return IotdbQueryRow.builder()
                    .time(resultSet.getLong("Time"))
                    .value(value)
                    .quality(safeGetInteger(resultSet, qualityCol))
                    .build();
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 最新值失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 查询指定时间范围内的指标序列。
     *
     * @param deviceId  设备ID
     * @param sensorCode  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @return 区间内的时序结果集合
     * @throws ServiceException 当查询失败时抛出
     */
    public List<IotdbQueryRow> queryRange(Long deviceId, String sensorCode, String attrCode, Long startTime, Long endTime) {
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(attrCode)
                .append(", quality FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorCode));
        if (startTime != null || endTime != null) {
            sql.append(" WHERE ");
            if (startTime != null) {
                sql.append("time >= ").append(startTime);
            }
            if (startTime != null && endTime != null) {
                sql.append(" AND ");
            }
            if (endTime != null) {
                sql.append("time < ").append(endTime);
            }
        }
        // IoTDB JDBC ResultSet 中列名为完整路径。
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, "quality");
        List<IotdbQueryRow> rows = new ArrayList<>();
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());
            while (resultSet.next()) {
                Double value = safeGetDouble(resultSet, attrCol);
                Integer quality = safeGetInteger(resultSet, qualityCol);
                if (value == null) {
                    continue;
                }
                rows.add(IotdbQueryRow.builder()
                        .time(resultSet.getLong("Time"))
                        .value(value)
                        .quality(quality)
                        .build());
            }
            return rows;
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 时间序列失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 查询指定时间范围内的指标序列（支持聚合）。
     *
     * @param deviceId  设备ID
     * @param sensorCode  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @param valueType 值类型（current=原始，hour/24h/72h=聚合）
     * @return 区间内的时序结果集合
     * @throws ServiceException 当查询失败时抛出
     */
    public List<IotdbQueryRow> queryRange(Long deviceId, String sensorCode, String attrCode,
                                          Long startTime, Long endTime, ValueType valueType) {
        if (valueType == null || !valueType.isAggregated()) {
            return queryRange(deviceId, sensorCode, attrCode, startTime, endTime);
        }
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        String sensorPath = pathResolver.buildSensorPath(deviceId, sensorCode);
        String aggFunc = valueType.getAggFunction();
        String interval = valueType.getGroupInterval();
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(aggFunc).append("(").append(attrCode).append("), ")
                .append(aggFunc).append("(quality) FROM ")
                .append(sensorPath);
        if (startTime != null && endTime != null) {
            sql.append(" WHERE time >= ").append(startTime)
                    .append(" AND time < ").append(endTime);
        } else if (startTime != null) {
            sql.append(" WHERE time >= ").append(startTime);
        } else if (endTime != null) {
            sql.append(" WHERE time < ").append(endTime);
        }
        sql.append(" GROUP BY ([")
                .append(startTime != null ? startTime : 0)
                .append(", ")
                .append(endTime != null ? endTime : System.currentTimeMillis())
                .append("), ").append(interval).append(")");
        List<IotdbQueryRow> rows = new ArrayList<>();
        // IoTDB 聚合查询列名：{aggFunc}({fullPath})
        String attrCol = aggFunc + "(" + pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode) + ")";
        String qualityCol = aggFunc + "(" + pathResolver.buildMeasurementPath(deviceId, sensorCode, "quality") + ")";
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());
            while (resultSet.next()) {
                Double value = safeGetDouble(resultSet, attrCol);
                Integer quality = safeGetInteger(resultSet, qualityCol);
                if (value == null) {
                    continue;
                }
                rows.add(IotdbQueryRow.builder()
                        .time(resultSet.getLong("Time"))
                        .value(value)
                        .quality(quality)
                        .build());
            }
            return rows;
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 聚合序列失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 分页查询指定时间范围内的指标序列，使用 IoTDB 原生 LIMIT/OFFSET。
     *
     * @param deviceId  设备ID
     * @param sensorCode  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @param limit     返回条数上限
     * @param offset    偏移量
     * @return 区间内的时序结果集合，按时间降序
     */
    public List<IotdbQueryRow> queryRangePaged(Long deviceId, String sensorCode, String attrCode,
                                               Long startTime, Long endTime, int limit, int offset) {
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorCode, "INT32", "RLE");
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(attrCode)
                .append(", quality FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorCode));
        boolean hasWhere = false;
        if (startTime != null) {
            sql.append(" WHERE time >= ").append(startTime);
            hasWhere = true;
        }
        if (endTime != null) {
            sql.append(hasWhere ? " AND " : " WHERE ").append("time < ").append(endTime);
        }
        sql.append(" ORDER BY TIME DESC LIMIT ").append(limit).append(" OFFSET ").append(offset);
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorCode, "quality");
        List<IotdbQueryRow> rows = new ArrayList<>();
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());
            while (resultSet.next()) {
                Double value = safeGetDouble(resultSet, attrCol);
                Integer quality = safeGetInteger(resultSet, qualityCol);
                if (value == null) {
                    continue;
                }
                rows.add(IotdbQueryRow.builder()
                        .time(resultSet.getLong("Time"))
                        .value(value)
                        .quality(quality)
                        .build());
            }
            return rows;
        } catch (SQLException e) {
            throw new ServiceException("查询 IoTDB 分页序列失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 统计指定测点指标在时间范围内的数据条数。
     *
     * @param deviceId  设备ID
     * @param sensorCode  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @return 数据条数
     */
    public long countRange(Long deviceId, String sensorCode, String attrCode, Long startTime, Long endTime) {
        ensureMeasurement(attrCode, deviceId, sensorCode, "DOUBLE", "GORILLA");
        StringBuilder sql = new StringBuilder("SELECT COUNT(")
                .append(attrCode)
                .append(") FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorCode));
        boolean hasWhere = false;
        if (startTime != null) {
            sql.append(" WHERE time >= ").append(startTime);
            hasWhere = true;
        }
        if (endTime != null) {
            sql.append(hasWhere ? " AND " : " WHERE ").append("time < ").append(endTime);
        }
        String countCol = "COUNT(" + pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode) + ")";
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());
            if (resultSet.next()) {
                return resultSet.getLong(countCol);
            }
            return 0;
        } catch (SQLException e) {
            log.warn("统计 IoTDB 数据条数失败: deviceId={}, sensorCode={}, attrCode={}", deviceId, sensorCode, attrCode, e);
            return 0;
        }
    }

    /**
     * 确保 IoTDB 数据库存在。
     *
     * <p>使用 {@code volatile databaseReady} 实现无锁双重检查：
     * <ol>
     *   <li>若 databaseReady=true → 直接返回（热路径，无锁）</li>
     *   <li>若 databaseReady=false → 检查数据库是否存在 → 不存在则 CREATE DATABASE</li>
     *   <li>设置 databaseReady=true，后续调用全部走快速路径</li>
     * </ol>
     */
    private void ensureDatabase() {
        if (databaseReady) {
            return;
        }
        if (databaseExists()) {
            databaseReady = true;
            log.info("数据库 {} 已存在，跳过建库", properties.getDatabase());
            return;
        }
        jdbcClient.executeSilent("CREATE DATABASE " + properties.getDatabase());
        databaseReady = true;
    }

    /**
     * 通过 JDBC 查询 IoTDB 中是否已存在目标数据库。
     */
    private boolean databaseExists() {
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
            ResultSet rs = statement.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                if (properties.getDatabase().equals(rs.getString(1))) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            log.debug("SHOW DATABASES 查询失败，回退到 CREATE DATABASE 尝试", e);
            return false;
        }
    }

    /**
     * 确保指定 measurement 已创建（非阻塞，ConcurrentHashMap 去重）。
     */
    private void ensureMeasurement(String attrCode,
                                   Long deviceId,
                                   String sensorCode,
                                   String dataType,
                                   String encoding) {
        String measurementPath = pathResolver.buildMeasurementPath(deviceId, sensorCode, attrCode);
        if (createdMeasurements.containsKey(measurementPath)) {
            return;
        }
        jdbcClient.executeSilent("CREATE TIMESERIES " + measurementPath
                + " WITH DATATYPE=" + dataType
                + ", ENCODING=" + encoding
                + ", COMPRESSOR=SNAPPY");
        createdMeasurements.putIfAbsent(measurementPath, Boolean.TRUE);
    }

    /**
     * 将查询结果值转换为双精度数值。
     *
     * @param value 原始值
     * @return 双精度数值；为空时返回 {@code null}
     * @throws NumberFormatException 当字符串无法转换为数字时抛出
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    /**
     * 将查询结果值转换为整型质量码。
     *
     * @param value 原始值
     * @return 整型质量码；为空时返回 {@code 0}
     * @throws NumberFormatException 当字符串无法转换为整数时抛出
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    /**
     * 安全获取 ResultSet 中的列值，兜底 IoTDB 2.0 对不存在的 measurement
     * 执行 SELECT 时 JDBC 驱动 getObject() 的 NPE。
     */
    private Double safeGetDouble(ResultSet rs, String column) {
        try {
            return toDouble(rs.getObject(column));
        } catch (Exception e) {
            log.debug("IoTDB 列不存在: {}", column);
            return null;
        }
    }

    private Integer safeGetInteger(ResultSet rs, String column) {
        try {
            return toInteger(rs.getObject(column));
        } catch (Exception e) {
            log.debug("IoTDB 列不存在: {}", column);
            return 0;
        }
    }
}
