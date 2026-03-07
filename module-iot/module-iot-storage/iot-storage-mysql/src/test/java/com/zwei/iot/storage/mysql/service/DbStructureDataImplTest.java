package com.zwei.iot.storage.mysql.service;

import com.zwei.iot.core.thing.domain.ThingModel;
import com.zwei.iot.core.thing.domain.TslProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DbStructureDataImpl的单元测试类
 */
public class DbStructureDataImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DbStructureDataImpl dbStructureDataImpl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDefineThingModel() {
        // 准备测试数据
        ThingModel thingModel = new ThingModel("test_product");

        thingModel.setProperties(new ArrayList<>());
        thingModel.setEvents(new ArrayList<>());
        
        // 执行测试
        dbStructureDataImpl.defineThingModel(thingModel);
        
        // 验证行为
        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void testUpdateThingModel_WhenTableExists() {
        // 准备测试数据
        ThingModel thingModel = new ThingModel("test_product");

        List<TslProperty> properties = new ArrayList<>();
        TslProperty property = new TslProperty("test_property");
        property.setIdentifier("test_property");
        properties.add(property);
        thingModel.setProperties(properties);

        // 模拟表存在
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Integer.class)))
                .thenReturn(1);

        // 执行测试
        dbStructureDataImpl.updateThingModel(thingModel);

        // 验证行为，在表存在时应该只进行一次表修改
        verify(jdbcTemplate, times(1)).execute(contains("ALTER TABLE"));
    }

    @Test
    void testUpdateThingModel_WhenTableDoesNotExist() {
        // 准备测试数据
        ThingModel thingModel = new ThingModel("test_product");

        thingModel.setProperties(new ArrayList<>());
        thingModel.setEvents(new ArrayList<>());
        
        // 模拟表不存在
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Integer.class)))
                .thenReturn(0);
        
        // 执行测试
        dbStructureDataImpl.updateThingModel(thingModel);
        
        // 验证行为
        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void testInitDbStructure() {
        // 执行测试
        dbStructureDataImpl.initDbStructure();
        
        // 验证行为
        verify(jdbcTemplate, times(1)).execute(anyString());
    }
}