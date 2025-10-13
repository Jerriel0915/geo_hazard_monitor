package com.zwei.iot.storage.mysql.service;

import com.zwei.iot.core.ThingModel;
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
        ThingModel thingModel = new ThingModel();
        thingModel.setProductKey("test_product");
        
        ThingModel.Model model = new ThingModel.Model();
        model.setProperties(new ArrayList<>());
        model.setEvents(new ArrayList<>());
        thingModel.setModel(model);
        
        // 执行测试
        dbStructureDataImpl.defineThingModel(thingModel);
        
        // 验证行为
        verify(jdbcTemplate, times(2)).execute(anyString());
    }

    @Test
    void testUpdateThingModel_WhenTableExists() {
        // 准备测试数据
        ThingModel thingModel = new ThingModel();
        thingModel.setProductKey("test_product");
        
        ThingModel.Model model = new ThingModel.Model();
        List<ThingModel.Property> properties = new ArrayList<>();
        ThingModel.Property property = new ThingModel.Property();
        property.setIdentifier("test_property");
        properties.add(property);
        model.setProperties(properties);
        thingModel.setModel(model);
        
        // 模拟表存在
        when(jdbcTemplate.queryForObject(anyString(), any(Object[].class), eq(Integer.class)))
                .thenReturn(1);
        
        // 执行测试
        dbStructureDataImpl.updateThingModel(thingModel);
        
        // 验证行为
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void testUpdateThingModel_WhenTableDoesNotExist() {
        // 准备测试数据
        ThingModel thingModel = new ThingModel();
        thingModel.setProductKey("test_product");
        
        ThingModel.Model model = new ThingModel.Model();
        model.setProperties(new ArrayList<>());
        model.setEvents(new ArrayList<>());
        thingModel.setModel(model);
        
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