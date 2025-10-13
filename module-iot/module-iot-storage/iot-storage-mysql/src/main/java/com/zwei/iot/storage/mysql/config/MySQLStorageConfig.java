package com.zwei.iot.storage.mysql.config;

import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.iot.storage.core.IDevicePropertyData;
import com.zwei.iot.storage.mysql.service.DbStructureDataImpl;
import com.zwei.iot.storage.mysql.service.DevicePropertyDataImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * MySQL存储模块配置类
 * 
 * @author linx
 * @date 2025-10-10
 */
@Configuration
@ConditionalOnClass(JdbcTemplateAutoConfiguration.class)
public class MySQLStorageConfig {
    
    /**
     * 配置数据库结构服务
     */
    @Bean
    @ConditionalOnMissingBean
    public IDbStructureData dbStructureData(JdbcTemplate jdbcTemplate) {
        return new DbStructureDataImpl(jdbcTemplate);
    }
    
    /**
     * 配置设备属性数据服务
     */
    @Bean
    @ConditionalOnMissingBean
    public IDevicePropertyData devicePropertyData(JdbcTemplate jdbcTemplate) {
        return new DevicePropertyDataImpl(jdbcTemplate);
    }
}