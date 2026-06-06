package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.timeseries.config.IotdbProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * IoTDB JDBC 访问封装。
 *
 * <p>IoTDB JDBC 连接与 SQL 执行适配层，统一为时序读写服务提供连接创建与异常包装能力。
 */
@Slf4j
@Component
public class IotdbJdbcClient {
    private static final String DRIVER = "org.apache.iotdb.jdbc.IoTDBDriver";

    private final IotdbProperties properties;

    /**
     * 构造 IoTDB JDBC 客户端。
     *
     * @param properties IoTDB 配置
     */
    @Autowired
    public IotdbJdbcClient(IotdbProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建 IoTDB JDBC 连接。
     *
     * @return JDBC 连接对象
     * @throws ServiceException 当 IoTDB 未启用或连接失败时抛出
     */
    public Connection getConnection() {
        if (!properties.isEnabled()) {
            throw new ServiceException("IoTDB 未启用");
        }
        try {
            Class.forName(DRIVER);
            DriverManager.setLoginTimeout(Math.max(1, properties.getConnectionTimeoutMs() / 1000));
            return DriverManager.getConnection(
                    properties.getJdbcUrl(),
                    properties.getUsername(),
                    properties.getPassword()
            );
        } catch (ClassNotFoundException | SQLException e) {
            throw new ServiceException("连接 IoTDB 失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 执行无返回结果的 IoTDB SQL。
     *
     * @param sql 待执行 SQL
     * @throws ServiceException 当 SQL 执行失败时抛出
     */
    public void execute(String sql) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
            statement.execute(sql);
        } catch (SQLException e) {
            log.error("执行 IoTDB SQL 失败, sql={}", sql, e);
            throw new ServiceException("执行 IoTDB SQL 失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 批量执行 IoTDB INSERT SQL。
     * <p>
     * 单连接批量提交，使用 JDBC {@code executeBatch()} 替代逐条 {@code execute()}，
     * 避免每条 INSERT 新建一次物理连接。IoTDB 官方文档推荐此方式以获得更高写入性能。
     *
     * @param sqlList 待执行的 INSERT 语句列表
     * @throws ServiceException 当批量执行失败时抛出
     */
    public void executeBatch(List<String> sqlList) {
        if (sqlList == null || sqlList.isEmpty()) {
            return;
        }
        if (!properties.isEnabled()) {
            throw new ServiceException("IoTDB 未启用");
        }
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
            for (String sql : sqlList) {
                statement.addBatch(sql);
            }
            statement.executeBatch();
            statement.clearBatch();
        } catch (SQLException e) {
            log.error("批量执行 IoTDB SQL 失败, size={}", sqlList.size(), e);
            throw new ServiceException("批量执行 IoTDB SQL 失败").setDetailMessage(e.getMessage());
        }
    }

    /**
     * 静默执行 IoTDB DDL（建库/建时序），失败仅 DEBUG 记录，不抛异常。
     * <p>
     * 用于资源已存在属预期场景（如建库/建时序时的幂等操作），
     *
     * @param sql 待执行 DDL
     */
    public void executeSilent(String sql) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(properties.getQueryTimeoutSeconds());
            statement.execute(sql);
        } catch (SQLException e) {
            // 仅记录 SQL 文本，不输出堆栈 —— 建库/建时序时资源已存在属 100% 预期场景
            log.debug("IoTDB DDL 已忽略: {}", sql);
        }
    }
}
