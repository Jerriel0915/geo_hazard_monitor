package com.zwei.iot.timeseries.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IoTDB 连接配置。
 *
 * <p>绑定前缀为 {@code iot.iotdb} 的配置项，用于支撑工业监测数据的时序化存储与查询。
 * 默认端口 6667，默认数据库 {@code root.geo_hazard}。</p>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li>{@code enabled} — 是否启用 IoTDB，默认为 true</li>
 *   <li>{@code host} — IoTDB 服务地址，默认 localhost</li>
 *   <li>{@code port} — IoTDB JDBC 端口，默认 6667</li>
 *   <li>{@code username} — 用户名，默认 root</li>
 *   <li>{@code password} — 密码，默认 root</li>
 *   <li>{@code database} — 目标数据库路径，默认 root.geo_hazard</li>
 *   <li>{@code connectionTimeoutMs} — 连接超时毫秒数，默认 5000</li>
 *   <li>{@code queryTimeoutSeconds} — 查询超时秒数，默认 30</li>
 *   <li>{@code fetchSize} — JDBC fetch size，默认 500</li>
 * </ul>
 *
 * <p>新增 IoTDB 连接参数绑定，用于支撑工业监测数据的时序化存储与查询。</p>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "iot.iotdb")
public class IotdbProperties {
    private boolean enabled = true;
    private String host = "localhost";
    private int port = 6667;
    private String username = "root";
    private String password = "root";
    private String database = "root.geo_hazard";
    private int connectionTimeoutMs = 5000;
    private int queryTimeoutSeconds = 30;
    private int fetchSize = 500;

    /**
     * 生成 IoTDB JDBC 连接地址。
     *
     * @return IoTDB JDBC URL
     */
    public String getJdbcUrl() {
        return "jdbc:iotdb://" + host + ":" + port + "/";
    }
}
