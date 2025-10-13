package com.zwei.iot.storage.mysql.config;

import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.iot.storage.core.IDevicePropertyData;
import com.zwei.iot.storage.mysql.service.DbStructureDataImpl;
import com.zwei.iot.storage.mysql.service.DevicePropertyDataImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MySQLStorageConfig的单元测试类
 */
@SpringBootTest(classes = {
        MySQLStorageConfig.class,
        JdbcTemplateAutoConfiguration.class
})
public class MySQLStorageConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDbStructureDataBeanCreation() {
        // 验证IDbStructureData类型的Bean存在
        assertTrue(applicationContext.containsBean("dbStructureData"));
        
        // 获取Bean并验证类型
        IDbStructureData dbStructureData = applicationContext.getBean(IDbStructureData.class);
        assertNotNull(dbStructureData);
        assertTrue(dbStructureData instanceof DbStructureDataImpl);
    }

    @Test
    void testDevicePropertyDataBeanCreation() {
        // 验证IDevicePropertyData类型的Bean存在
        assertTrue(applicationContext.containsBean("devicePropertyData"));
        
        // 获取Bean并验证类型
        IDevicePropertyData devicePropertyData = applicationContext.getBean(IDevicePropertyData.class);
        assertNotNull(devicePropertyData);
        assertTrue(devicePropertyData instanceof DevicePropertyDataImpl);
    }

    @Test
    void testBeanUniqueness() {
        // 验证不存在重复的Bean定义
        String[] dbStructureDataBeanNames = applicationContext.getBeanNamesForType(IDbStructureData.class);
        assertEquals(1, dbStructureDataBeanNames.length);
        
        String[] devicePropertyDataBeanNames = applicationContext.getBeanNamesForType(IDevicePropertyData.class);
        assertEquals(1, devicePropertyDataBeanNames.length);
    }

    @Test
    void testConditionalOnClassWorking() {
        // 验证当JdbcTemplateAutoConfiguration存在时，配置类被正确加载
        // 这通过前面的测试已经间接验证，这里主要是确认配置生效
        assertNotNull(applicationContext.getBean(MySQLStorageConfig.class));
    }
}