package com.zwei.iot.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 报告管理模块 Spring 配置。
 * MyBatis Mapper 扫描 com.zwei.iot.report.mapper。
 */
@Configuration
@MapperScan("com.zwei.iot.report.mapper")
public class ReportModuleConfig {
}
