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
import java.util.*;

/**
 * IoTDB 树模型读写服务，用于完成监测数据的动态建模、写入、最新值查询与区间查询。
 */
@Slf4j
@Service
public class IotdbTimeSeriesService {
    private final IotdbJdbcClient jdbcClient;
    private final IotdbProperties properties;
    private final IotdbPathResolver pathResolver;
    private final Set<String> createdMeasurements = new HashSet<>();
    private boolean databaseReady;

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
     * 批量写入标准化时序点。
     *
     * @param points 标准化时序点集合
     * @throws ServiceException 当 IoTDB 写入失败时抛出
     */
    public void writePoints(Collection<StandardMeasurementPoint> points) {
        if (points == null || points.isEmpty()) {
            return;
        }
        ensureDatabase();
        for (StandardMeasurementPoint point : points) {
            ensureMeasurement(point.attrCode(), point.deviceId(), point.sensorNo(), "DOUBLE", "GORILLA");
            ensureMeasurement("quality", point.deviceId(), point.sensorNo(), "INT32", "RLE");
            String sql = "INSERT INTO " + pathResolver.buildSensorPath(point.deviceId(), point.sensorNo())
                    + "(timestamp," + point.attrCode() + ",quality) ALIGNED VALUES("
                    + point.dataTime() + "," + point.value() + "," + point.quality() + ")";
            jdbcClient.execute(sql);
        }
    }

    /**
     * 为指定设备传感器预创建 IoTDB 时序 schema。
     * <p>
     * 在设备/传感器注册时调用，将建库建时序从写入热路径提前至注册冷路径，
     * 避免每条消息写入时触发 DDL 的 ERROR 日志。
     *
     * @param deviceId  设备ID
     * @param sensorNo  传感器编号
     * @param attrCodes 指标编码列表（不含 quality，quality 列自动创建）
     */
    public void createSensorSchema(Long deviceId, String sensorNo, List<String> attrCodes) {
        // 确保数据库表存在
        ensureDatabase();
        ensureMeasurement("quality", deviceId, sensorNo, "INT32", "RLE");
        for (String attrCode : attrCodes) {
            ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        }
    }

    /**
     * 查询指定测点指标的最新值。
     *
     * @param deviceId 设备ID
     * @param sensorNo 传感器编号
     * @param attrCode 指标编码
     * @return 最新查询结果；无数据时返回 {@code null}
     * @throws ServiceException 当查询失败时抛出
     */
    public IotdbQueryRow queryLatest(Long deviceId, String sensorNo, String attrCode) {
        ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorNo, "INT32", "RLE");
        String sql = "SELECT " + attrCode + ", quality FROM " + pathResolver.buildSensorPath(deviceId, sensorNo)
                + " ORDER BY TIME DESC LIMIT 1";
        // IoTDB JDBC ResultSet 中列名为完整路径，需用 buildMeasurementPath 构造。
        // 同时移除 setFetchSize / setQueryTimeout（IoTDB JDBC 不支持）。
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, "quality");
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
     * @param sensorNo  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @return 区间内的时序结果集合
     * @throws ServiceException 当查询失败时抛出
     */
    public List<IotdbQueryRow> queryRange(Long deviceId, String sensorNo, String attrCode, Long startTime, Long endTime) {
        ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorNo, "INT32", "RLE");
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(attrCode)
                .append(", quality FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorNo));
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
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, "quality");
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
     * @param sensorNo  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @param valueType 值类型（current=原始，hour/24h/72h=聚合）
     * @return 区间内的时序结果集合
     * @throws ServiceException 当查询失败时抛出
     */
    public List<IotdbQueryRow> queryRange(Long deviceId, String sensorNo, String attrCode,
                                          Long startTime, Long endTime, ValueType valueType) {
        if (valueType == null || !valueType.isAggregated()) {
            return queryRange(deviceId, sensorNo, attrCode, startTime, endTime);
        }
        ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorNo, "INT32", "RLE");
        String sensorPath = pathResolver.buildSensorPath(deviceId, sensorNo);
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
        String attrCol = aggFunc + "(" + pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode) + ")";
        String qualityCol = aggFunc + "(" + pathResolver.buildMeasurementPath(deviceId, sensorNo, "quality") + ")";
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
     * @param sensorNo  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @param limit     返回条数上限
     * @param offset    偏移量
     * @return 区间内的时序结果集合，按时间降序
     */
    public List<IotdbQueryRow> queryRangePaged(Long deviceId, String sensorNo, String attrCode,
                                               Long startTime, Long endTime, int limit, int offset) {
        ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        ensureMeasurement("quality", deviceId, sensorNo, "INT32", "RLE");
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(attrCode)
                .append(", quality FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorNo));
        boolean hasWhere = false;
        if (startTime != null) {
            sql.append(" WHERE time >= ").append(startTime);
            hasWhere = true;
        }
        if (endTime != null) {
            sql.append(hasWhere ? " AND " : " WHERE ").append("time < ").append(endTime);
        }
        sql.append(" ORDER BY TIME DESC LIMIT ").append(limit).append(" OFFSET ").append(offset);
        String attrCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode);
        String qualityCol = pathResolver.buildMeasurementPath(deviceId, sensorNo, "quality");
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
     * @param sensorNo  传感器编号
     * @param attrCode  指标编码
     * @param startTime 开始时间，毫秒时间戳，可空
     * @param endTime   结束时间，毫秒时间戳，可空
     * @return 数据条数
     */
    public long countRange(Long deviceId, String sensorNo, String attrCode, Long startTime, Long endTime) {
        ensureMeasurement(attrCode, deviceId, sensorNo, "DOUBLE", "GORILLA");
        StringBuilder sql = new StringBuilder("SELECT COUNT(")
                .append(attrCode)
                .append(") FROM ")
                .append(pathResolver.buildSensorPath(deviceId, sensorNo));
        boolean hasWhere = false;
        if (startTime != null) {
            sql.append(" WHERE time >= ").append(startTime);
            hasWhere = true;
        }
        if (endTime != null) {
            sql.append(hasWhere ? " AND " : " WHERE ").append("time < ").append(endTime);
        }
        String countCol = "COUNT(" + pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode) + ")";
        try (Connection connection = jdbcClient.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql.toString());
            if (resultSet.next()) {
                return resultSet.getLong(countCol);
            }
            return 0;
        } catch (SQLException e) {
            log.warn("统计 IoTDB 数据条数失败: deviceId={}, sensorNo={}, attrCode={}", deviceId, sensorNo, attrCode, e);
            return 0;
        }
    }

    /**
     * 确保 IoTDB 数据库存在。
     *
     * @throws ServiceException 当底层执行建库语句失败且未被忽略时抛出
     */
    private synchronized void ensureDatabase() {
        if (databaseReady) {
            return;
        }
        // 先通过 SHOW DATABASES 预检，已存在则跳过；
        // 不存在时用 executeSilent 创建（失败仅 DEBUG 记录，不抛异常）。
        if (databaseExists()) {
            log.info("数据库 {} 已存在，跳过建库", properties.getDatabase());
            databaseReady = true;
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
     * 确保指定 measurement 已创建。
     *
     * @param attrCode 指标编码
     * @param deviceId 设备ID
     * @param sensorNo 传感器编号
     * @param dataType IoTDB 数据类型
     * @param encoding IoTDB 编码方式
     */
    private synchronized void ensureMeasurement(String attrCode,
                                                Long deviceId,
                                                String sensorNo,
                                                String dataType,
                                                String encoding) {
        String measurementPath = pathResolver.buildMeasurementPath(deviceId, sensorNo, attrCode);
        if (createdMeasurements.contains(measurementPath)) {
            return;
        }
        // executeSilent：成功则静默，已存在则 DEBUG 记录
        jdbcClient.executeSilent("CREATE TIMESERIES " + measurementPath
                + " WITH DATATYPE=" + dataType
                + ", ENCODING=" + encoding
                + ", COMPRESSOR=SNAPPY");
        createdMeasurements.add(measurementPath);
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
