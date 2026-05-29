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
}
