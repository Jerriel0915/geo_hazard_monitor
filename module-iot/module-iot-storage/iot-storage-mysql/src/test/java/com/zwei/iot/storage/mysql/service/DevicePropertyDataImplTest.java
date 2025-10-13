package com.zwei.iot.storage.mysql.service;

import com.zwei.iot.core.DeviceProperty;
import com.zwei.iot.core.DevicePropertyCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DevicePropertyDataImpl的单元测试类
 */
public class DevicePropertyDataImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DevicePropertyDataImpl devicePropertyDataImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFindDevicePropertyHistory() {
        // 准备测试数据
        Long deviceId = 1L;
        String propertyName = "temperature";
        long startTime = 1600000000000L;
        long endTime = 1610000000000L;
        int size = 10;
        
        // 模拟查询结果
        List<Map<String, Object>> mockResult = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("device_id", deviceId.toString());
        row1.put("property_name", propertyName);
        row1.put("property_value", "25.5");
        row1.put("report_time", 1605000000000L);
        mockResult.add(row1);
        
        when(jdbcTemplate.queryForList(anyString(), eq(deviceId.toString()), eq(propertyName), 
                eq(startTime), eq(endTime), eq(size))).thenReturn(mockResult);
        
        // 执行测试
        List<DeviceProperty> result = devicePropertyDataImpl.findDevicePropertyHistory(
                deviceId, propertyName, startTime, endTime, size);
        
        // 验证结果
        assertEquals(1, result.size());
        assertEquals(deviceId.toString(), result.get(0).getDeviceId());
        assertEquals(propertyName, result.get(0).getName());
        assertEquals("25.5", result.get(0).getValue());
    }

    @Test
    void testAddProperties() {
        // 准备测试数据
        Long deviceId = 1L;
        long time = 1605000000000L;
        
        Map<String, DevicePropertyCache> properties = new HashMap<>();
        DevicePropertyCache cache1 = new DevicePropertyCache();
        cache1.setValue(25.5);
        properties.put("temperature", cache1);
        
        DevicePropertyCache cache2 = new DevicePropertyCache();
        cache2.setValue(60.0);
        properties.put("humidity", cache2);
        
        // 执行测试
        devicePropertyDataImpl.addProperties(deviceId, properties, time);
        
        // 验证行为
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), anyList());
        verify(jdbcTemplate, times(1)).update(anyString(), eq(deviceId.toString()), eq(time), eq(time));
    }

    @Test
    void testAddProperties_WithEmptyMap() {
        // 准备测试数据
        Long deviceId = 1L;
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
        devicePropertyDataImpl.addProperties(1L, null, 1605000000000L);
        
        // 验证没有执行数据库操作
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}