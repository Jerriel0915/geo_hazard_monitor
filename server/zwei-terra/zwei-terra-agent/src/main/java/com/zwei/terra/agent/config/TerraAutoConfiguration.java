package com.zwei.terra.agent.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Terra Agent 自动装配入口。
 *
 * <p>被 META-INF/spring/AutoConfiguration.imports 引用，
 * 确保 terra-agent 模块的 Bean 在宿主应用中自动注册。</p>
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.zwei.terra")
public class TerraAutoConfiguration {
}
