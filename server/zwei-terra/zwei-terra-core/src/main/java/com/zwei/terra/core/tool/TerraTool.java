package com.zwei.terra.core.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Terra 工具注解，标记一个类为 Terra 后端工具。
 * 业务模块实现 TerraBackendTool 接口并加此注解，
 * terra-agent 的 ToolManager 会自动扫描注册。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TerraTool {
    String name();
    String description();
    String category() default "general";
}
