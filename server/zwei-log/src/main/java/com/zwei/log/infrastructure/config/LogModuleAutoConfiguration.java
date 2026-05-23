package com.zwei.log.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 日志模块自动配置
 *
 * @author zwei
 */
@Configuration
@EnableConfigurationProperties(LogModuleProperties.class)
public class LogModuleAutoConfiguration {
}
