package com.zwei.terra.agent.tool;

import com.zwei.terra.core.tool.ToolDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Method;

/**
 * 工具注册信息 — 描述一个已注册工具的元数据与执行入口。
 * <ul>
 *   <li>code 工具: bean + method 用于反射调用</li>
 *   <li>config 工具: endpoint 用于 HTTP 调用</li>
 * </ul>
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRegistration {
    private String toolKey;
    private ToolDefinition definition;
    /** code 工具的执行入口 */
    private Object bean;
    private Method method;
    /** config 工具的执行入口 */
    private String endpoint;

    public boolean isCodeTool() {
        return bean != null;
    }
}
