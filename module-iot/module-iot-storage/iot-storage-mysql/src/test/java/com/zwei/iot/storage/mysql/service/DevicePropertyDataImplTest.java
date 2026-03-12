package com.zwei.iot.storage.mysql.service;

import com.zwei.iot.core.thing.domain.DeviceProperty;
import com.zwei.iot.core.thing.domain.DevicePropertyCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DevicePropertyDataImpl的单元测试类
 */
public class DevicePropertyDataImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private Logger log;

    @InjectMocks
    private DevicePropertyDataImpl devicePropertyDataImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindDevicePropertyHistory() {
        // 准备测试数据
        String deviceId = "1";
        String propertyName = "temperature";
        long startTime = 1600000000000L;
        long endTime = 1610000000000L;
        int size = 10;

        // 模拟查询结果
        List<Map<String, Object>> mockResult = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("device_id", deviceId);
        row1.put("property_name", propertyName);
        row1.put("property_value", "25.5");
        row1.put("report_time", 1605000000000L);
        mockResult.add(row1);

        when(jdbcTemplate.queryForList(anyString(), eq(deviceId), eq(propertyName),
                eq(startTime), eq(endTime), eq(size))).thenReturn(mockResult);

        // 执行测试
        List<DeviceProperty> result = devicePropertyDataImpl.findDevicePropertyHistory(
                deviceId, propertyName, startTime, endTime, size);

        // 验证结果
        assertEquals(1, result.size());
        assertEquals(deviceId, result.get(0).getDeviceId());
        assertEquals(propertyName, result.get(0).getName());
        assertEquals("25.5", result.get(0).getValue());
    }

    @Test
    void testAddProperties() {
        // 准备测试数据
        String deviceId = "1";
        long time = 1605000000000L;

        Map<String, DevicePropertyCache> properties = new HashMap<>();
        DevicePropertyCache cache1 = new DevicePropertyCache();
        cache1.setValue(25.5);
        properties.put("temperature", cache1);

        DevicePropertyCache cache2 = new DevicePropertyCache();
        cache2.setValue(60.0);
        properties.put("humidity", cache2);

        // 假设表存在
        when(jdbcTemplate.queryForObject(
                eq("SELECT product_key FROM zw_iot_product WHERE id = ?"),
                any(Object[].class),
                eq(String.class)
        )).thenReturn("testProductKey");
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = ?"),
                any(Object[].class),
                eq(Integer.class)
        )).thenReturn(1);

        // 执行测试
        devicePropertyDataImpl.addProperties(deviceId, properties, time);

        // 验证行为
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
    }

    @Test
    void testAddProperties_WithEmptyMap() {
        // 准备测试数据
        String deviceId = "1";
        Map<String, DevicePropertyCache> properties = Collections.emptyMap();
        long time = 1605000000000L;

        // 执行测试
        devicePropertyDataImpl.addProperties(deviceId, properties, time);

        // 验证没有执行数据库操作
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void testAddProperties_WithNullParams() {
        // 执行测试 - 空设备ID
        devicePropertyDataImpl.addProperties(null, new HashMap<>(), 1605000000000L);

        // 执行测试 - 空属性
        devicePropertyDataImpl.addProperties("1", null, 1605000000000L);

        // 验证没有执行数据库操作
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void createDeviceProperty_Success() {
        // Given
        String productKey = "testProduct123";
        String expectedTableName = "zw_iot_device_property_testProduct123";

        // 模拟查询产品存在
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(id) FROM zw_iot_product WHERE product_key = ?"),
                eq(new Object[]{productKey}),
                eq(String.class)
        )).thenReturn("1");

        // When
        devicePropertyDataImpl.createDeviceProperty(productKey);

        // Then
        // 验证查询产品是否存在被调用
        verify(jdbcTemplate).queryForObject(
                eq("SELECT COUNT(id) FROM zw_iot_product WHERE product_key = ?"),
                eq(new Object[]{productKey}),
                eq(String.class)
        );

        // 验证创建表被调用
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS " + expectedTableName));
    }

    @Test
    void createDeviceProperty_ProductKeyIsNull() {
        // Given
        String productKey = null;

        // When
        devicePropertyDataImpl.createDeviceProperty(productKey);

        // 验证没有进行数据库查询和建表操作
        verify(jdbcTemplate, never()).queryForObject(anyString(), any(), any(Class.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void createDeviceProperty_ProductKeyIsEmpty() {
        // Given
        String productKey = "";

        // When
        devicePropertyDataImpl.createDeviceProperty(productKey);

        verify(jdbcTemplate, never()).queryForObject(anyString(), any(), any(Class.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void createDeviceProperty_ProductNotFound_ShouldThrowException() {
        // Given
        String productKey = "nonExistentProduct";

        when(jdbcTemplate.queryForObject(
                anyString(),
                any(),
                eq(String.class)
        )).thenReturn("0");

        // When
        devicePropertyDataImpl.createDeviceProperty(productKey);

        // 验证查询被执行，但建表没有执行
        verify(jdbcTemplate).queryForObject(anyString(), any(), eq(String.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void createDeviceProperty_SqlInjectionAttempt() {
        // Given - 尝试SQL注入
        String maliciousProductKey = "test'; DROP TABLE zw_iot_product; --";

        when(jdbcTemplate.queryForObject(anyString(), any(), eq(String.class)))
                .thenReturn("1");

        // When
        devicePropertyDataImpl.createDeviceProperty(maliciousProductKey);

        // Then 查表和建表都不应该执行
        verify(jdbcTemplate, never()).queryForObject(anyString(), any(), any(Class.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }
}