package com.zwei.iot.timeseries.util;

import com.zwei.iot.timeseries.config.IotdbProperties;
import com.zwei.iot.timeseries.domain.SensorSnapshot;
import com.zwei.iot.timeseries.service.IotdbJdbcClient;
import com.zwei.iot.timeseries.support.IotdbPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SensorDataQueryUtil — 传感器时刻数据查询")
class SensorDataQueryUtilTest {

    @Mock private IotdbJdbcClient jdbcClient;
    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private ResultSet resultSet;
    @Mock private ResultSetMetaData metaData;

    private IotdbPathResolver pathResolver;

    @BeforeEach
    void setUp() throws Exception {
        IotdbProperties props = new IotdbProperties();
        props.setDatabase("root.zwei");
        pathResolver = new IotdbPathResolver(props);

        when(jdbcClient.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
    }

    // ==================== 全属性查询（attrCode 为空） ====================

    @Test
    @DisplayName("doQuery — attrCode 为空：SELECT *，排除 quality 列与 null 值")
    void doQuery_allAttrs_excludesQualityAndNull() throws Exception {
        String rainfallCol = "root.zwei.d1.srain_01.rainfall";
        String tempCol = "root.zwei.d1.srain_01.temperature";
        String humCol = "root.zwei.d1.srain_01.humidity";
        String qCol = "root.zwei.d1.srain_01.quality";
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(5);
        when(metaData.getColumnLabel(1)).thenReturn("Time");
        when(metaData.getColumnLabel(2)).thenReturn(rainfallCol);
        when(metaData.getColumnLabel(3)).thenReturn(tempCol);
        when(metaData.getColumnLabel(4)).thenReturn(humCol);
        when(metaData.getColumnLabel(5)).thenReturn(qCol);
        when(resultSet.getObject(rainfallCol)).thenReturn(12.5);
        when(resultSet.getObject(tempCol)).thenReturn(25.3);
        when(resultSet.getObject(humCol)).thenReturn(null);
        when(resultSet.getObject(qCol)).thenReturn(0);

        SensorSnapshot snap = SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "rain_01", 1700000000000L, null);

        assertThat(snap).isNotNull();
        assertThat(snap.getTime()).isEqualTo(1700000000000L);
        assertThat(snap.getValues())
                .containsEntry("rainfall", 12.5)
                .containsEntry("temperature", 25.3)
                .hasSize(2);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("SELECT * FROM root.zwei.d1.srain_01")
                        && sql.contains("time <= 1700000000000")));
    }

    // ==================== 单属性查询 ====================

    @Test
    @DisplayName("doQuery — 单属性查询：SQL 含 time<= 与 ORDER BY TIME DESC LIMIT 1，返回快照")
    void doQuery_singleAttr_returnsSnapshot() throws Exception {
        String rainfallCol = "root.zwei.d1.srain_01.rainfall";
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("Time")).thenReturn(1700000000000L);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("Time");
        when(metaData.getColumnLabel(2)).thenReturn(rainfallCol);
        when(resultSet.getObject(rainfallCol)).thenReturn(12.5);

        SensorSnapshot snap = SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "rain_01", 1700000000000L, "rainfall");

        assertThat(snap).isNotNull();
        assertThat(snap.getTime()).isEqualTo(1700000000000L);
        assertThat(snap.getValues()).containsEntry("rainfall", 12.5);
        verify(statement).executeQuery(argThat((String sql) ->
                sql.contains("SELECT rainfall FROM root.zwei.d1.srain_01")
                        && sql.contains("time <= 1700000000000")
                        && sql.contains("ORDER BY TIME DESC")
                        && sql.contains("LIMIT 1")));
    }

    // ==================== 边界：无数据 ====================

    @Test
    @DisplayName("doQuery — 无数据（rs.next()=false）返回 null")
    void doQuery_noData_returnsNull() throws Exception {
        when(resultSet.next()).thenReturn(false);

        SensorSnapshot snap = SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "rain_01", 1700000000000L, "rainfall");

        assertThat(snap).isNull();
    }

    // ==================== 边界：参数校验 ====================

    @Test
    @DisplayName("doQuery — deviceId 为 null 抛 IllegalArgumentException")
    void doQuery_deviceIdNull_throws() {
        assertThatThrownBy(() -> SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, null, "rain_01", 1700000000000L, "rainfall"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deviceId");
    }

    @Test
    @DisplayName("doQuery — sensorCode 为空抛 IllegalArgumentException")
    void doQuery_sensorCodeBlank_throws() {
        assertThatThrownBy(() -> SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "", 1700000000000L, "rainfall"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sensorCode");
    }

    @Test
    @DisplayName("doQuery — attrCode 含非法字符（防 SQL 注入）抛 IllegalArgumentException")
    void doQuery_attrCodeInvalid_throws() {
        assertThatThrownBy(() -> SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "rain_01", 1700000000000L, "a; DROP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attrCode");
    }

    @Test
    @DisplayName("doQuery — attrCode 含空格抛 IllegalArgumentException")
    void doQuery_attrCodeWithSpace_throws() {
        assertThatThrownBy(() -> SensorDataQueryUtil.doQuery(
                jdbcClient, pathResolver, 1L, "rain_01", 1700000000000L, "a b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attrCode");
    }
}
