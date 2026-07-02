package com.zwei.iot.timeseries.service;

import com.zwei.common.exception.ServiceException;
import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * IoTDB JDBC 访问封装（HikariCP 连接池）。
 *
 * <p>IoTDB JDBC 连接与 SQL 执行适配层，统一为时序读写服务提供连接获取与异常包装能力。
 * 内部维护 HikariCP 连接池，避免每次 {@code getConnection()} 新建物理连接的握手开销，
 * 显著提升高频写入（监测数据接入）与并发查询的吞吐量。
 *
 * <p><b>连接池生命周期</b>：Bean 初始化时懒创建连接池，{@code @PreDestroy} 时关闭。
 * {@code enabled=false} 时不创建连接池，{@code getConnection()} 直接抛 {@link ServiceException}。
 */
@Slf4j
@Component
public class IotdbJdbcClient {
    private static final String DRIVER = "org.apache.iotdb.jdbc.IoTDBDriver";

    private final IotdbProperties properties;

    /**
     * HikariCP 连接池实例。
     * <p>懒初始化：首次 {@link #getConnection()} 时创建，{@code enabled=false} 时保持 {@code null}。
     * 使用 {@code volatile} 保证多线程可见性（虽实际在单线程消费路径首次创建，但查询路径多线程）。
     */
    private volatile HikariDataSource dataSource;

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
     * 创建 IoTDB JDBC 连接（从连接池借用）。
     *
     * <p>首次调用时懒初始化 HikariCP 连接池。连接用毕后由调用方通过 try-with-resources 归还连接池。
     *
     * @return JDBC 连接对象
     * @throws ServiceException 当 IoTDB 未启用或连接池初始化/获取连接失败时抛出
     */
    public Connection getConnection() {
        if (!properties.isEnabled()) {
            throw new ServiceException("IoTDB 未启用");
        }
        try {
            HikariDataSource pool = ensureDataSource();
            return pool.getConnection();
        } catch (SQLException e) {
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

    /**
     * 关闭连接池，供 Spring 容器销毁时调用。
     * <p>幂等：多次调用安全，已关闭后为 no-op。
     */
    @jakarta.annotation.PreDestroy
    void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("关闭 IoTDB HikariCP 连接池 (poolName={})", dataSource.getPoolName());
            dataSource.close();
        }
    }

    /**
     * 懒初始化 HikariCP 连接池（双重检查锁）。
     * <p>同步块防止并发首调用创建多个连接池。{@code volatile} 字段保证可见性。
     *
     * @return 连接池实例
     * @throws SQLException 当驱动加载失败时抛出
     */
    private HikariDataSource ensureDataSource() throws SQLException {
        HikariDataSource pool = dataSource;
        if (pool != null && !pool.isClosed()) {
            return pool;
        }
        synchronized (this) {
            pool = dataSource;
            if (pool != null && !pool.isClosed()) {
                return pool;
            }
            pool = createDataSource();
            dataSource = pool;
            return pool;
        }
    }

    /**
     * 构建 HikariCP 连接池配置并创建数据源。
     *
     * @return HikariDataSource 实例
     * @throws SQLException 当 JDBC 驱动加载失败时抛出
     */
    private HikariDataSource createDataSource() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("IoTDB JDBC 驱动未找到: " + DRIVER, e);
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName("iotdb-hikari");
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(DRIVER);
        config.setMaximumPoolSize(properties.getPoolMaxSize());
        config.setMinimumIdle(properties.getPoolMinIdle());
        config.setConnectionTimeout(properties.getConnectionTimeoutMs());
        config.setIdleTimeout(properties.getPoolIdleTimeoutSeconds() * 1000L);
        config.setMaxLifetime(properties.getPoolMaxLifetimeSeconds() * 1000L);
        // IoTDB 无传统 SELECT 1 心跳查询；连接保活依赖 TCP keepalive + pool 的 isValid 检测
        // 保持默认的 connectionTestQuery 为 null，HikariCP 使用 JDBC4 Connection.isValid()
        log.info("初始化 IoTDB HikariCP 连接池: jdbcUrl={}, maxSize={}, minIdle={}, connTimeoutMs={}",
                properties.getJdbcUrl(),
                properties.getPoolMaxSize(),
                properties.getPoolMinIdle(),
                properties.getConnectionTimeoutMs());
        return new HikariDataSource(config);
    }

    /**
     * 供测试注入 mock DataSource 使用。
     *
     * @param dataSource 数据源（可为 mock）
     */
    void setDataSource(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 供测试获取内部数据源实例。
     *
     * @return 内部 HikariDataSource；未初始化时返回 {@code null}
     */
    DataSource getDataSource() {
        return dataSource;
    }
}
