package com.zwei.terra.agent.tool;

import com.zwei.terra.agent.domain.TerraTool;
import com.zwei.terra.agent.mapper.TerraToolMapper;
import com.zwei.terra.core.tool.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具管理器 — 负责工具的注册、发现与执行。
 * <p>
 * 核心职责：
 * <ol>
 *   <li>启动时扫描 Spring 容器中所有 @TerraTool 标注的 Bean，解析 @ToolMethod 方法构建 ToolDefinition</li>
 *   <li>从数据库 terra_tool 表加载 config 工具</li>
 *   <li>提供 getEnabledToolDefinitions() 供 ChatService 使用</li>
 *   <li>提供 execute() 方法执行后端工具（反射调用或 HTTP 调用）</li>
 *   <li>提供 isFrontendTool() 判断是否为前端工具</li>
 * </ol>
 */
@Component
@Slf4j
public class ToolManager {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TerraToolMapper toolMapper;

    /** code 工具注册表: toolKey -> ToolRegistration */
    private final Map<String, ToolRegistration> codeTools = new ConcurrentHashMap<>();

    /** config 工具注册表: toolKey -> ToolRegistration */
    private final Map<String, ToolRegistration> configTools = new ConcurrentHashMap<>();

    private final ParameterNameDiscoverer paramNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 启动时扫描所有 @TerraTool 标注的 Bean，注册其 @ToolMethod 方法。
     */
    @PostConstruct
    public void scanCodeTools() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(
            com.zwei.terra.core.tool.TerraTool.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            com.zwei.terra.core.tool.TerraTool annotation = bean.getClass().getAnnotation(
                com.zwei.terra.core.tool.TerraTool.class);
            if (annotation == null) {
                continue;
            }
            String toolKey = annotation.name();

            for (Method method : bean.getClass().getDeclaredMethods()) {
                ToolMethod tm = method.getAnnotation(ToolMethod.class);
                if (tm == null) {
                    continue;
                }
                String methodKey = toolKey + "." + method.getName();

                ToolDefinition def = ToolDefinition.builder()
                    .name(methodKey)
                    .description(tm.description())
                    .execSide("backend")
                    .parametersSchema(buildParameterSchema(method))
                    .build();

                codeTools.put(methodKey, ToolRegistration.builder()
                    .toolKey(methodKey)
                    .definition(def)
                    .bean(bean)
                    .method(method)
                    .build());
                log.info("Registered code tool: {}", methodKey);
            }
            ensureCodeToolInDb(toolKey, annotation);
        }
        log.info("ToolManager scanned {} code tools", codeTools.size());
    }

    /**
     * 从数据库加载 config 来源的已启用工具。
     */
    public void loadConfigTools() {
        configTools.clear();
        List<TerraTool> tools = toolMapper.selectEnabled();
        for (TerraTool tool : tools) {
            if ("config".equals(tool.getSource())) {
                ToolDefinition.ToolDefinitionBuilder builder = ToolDefinition.builder()
                    .name(tool.getToolKey())
                    .description(tool.getDescription())
                    .execSide(tool.getExecSide());

                // 解析 DB 中的 parameters_schema JSON
                if (tool.getParametersSchema() != null && !tool.getParametersSchema().isBlank()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        Map<String, Object> schema = mapper.readValue(tool.getParametersSchema(),
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                        builder.parametersSchema(schema);
                    } catch (Exception e) {
                        log.warn("解析工具 parameters_schema 失败: tool={}, error={}", tool.getToolKey(), e.getMessage());
                    }
                }

                ToolDefinition def = builder.build();
                configTools.put(tool.getToolKey(), ToolRegistration.builder()
                    .toolKey(tool.getToolKey())
                    .definition(def)
                    .endpoint(tool.getEndpoint())
                    .build());
            }
        }
    }

    /**
     * 获取所有已启用工具的定义列表（code + config），供 LLM 工具声明使用。
     */
    public List<ToolDefinition> getEnabledToolDefinitions() {
        loadConfigTools();
        List<ToolDefinition> result = new ArrayList<>();
        for (ToolRegistration reg : codeTools.values()) {
            result.add(reg.getDefinition());
        }
        result.addAll(configTools.values().stream()
            .map(ToolRegistration::getDefinition).toList());
        return result;
    }

    /**
     * 执行指定工具。code 工具走反射调用，config 工具走 HTTP 调用。
     */
    public ToolResult execute(String toolKey, Map<String, Object> params) {
        ToolRegistration reg = codeTools.get(toolKey);
        if (reg != null && reg.isCodeTool()) {
            try {
                Object result = reg.getMethod().invoke(reg.getBean(),
                    mapParameters(reg.getMethod(), params));
                return ToolResult.success(result);
            } catch (Exception e) {
                log.error("Tool execution failed: {}", toolKey, e);
                return ToolResult.failure(
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }
        reg = configTools.get(toolKey);
        if (reg != null) {
            return executeHttpTool(reg, params);
        }
        return ToolResult.failure("Tool not found: " + toolKey);
    }

    /**
     * 判断是否为前端执行工具。
     */
    public boolean isFrontendTool(String toolKey) {
        ToolRegistration reg = configTools.get(toolKey);
        return reg != null && "frontend".equals(reg.getDefinition().getExecSide());
    }

    /**
     * 获取工具超时时间（秒），默认 30 秒。
     */
    public int getTimeoutSeconds(String toolKey) {
        TerraTool tool = toolMapper.selectByKey(toolKey);
        return tool != null && tool.getTimeoutSeconds() != null ? tool.getTimeoutSeconds() : 30;
    }

    // ==================== private methods ====================

    private ToolResult executeHttpTool(ToolRegistration reg, Map<String, Object> params) {
        // 配置化工具通过 HTTP 调用 endpoint
        // 暂时返回未实现，后续完善
        log.warn("HTTP tool execution not yet implemented for: {}", reg.getToolKey());
        return ToolResult.failure("HTTP tool execution not yet implemented");
    }

    private void ensureCodeToolInDb(String toolKey, com.zwei.terra.core.tool.TerraTool annotation) {
        try {
            TerraTool existing = toolMapper.selectByKey(toolKey);
            if (existing == null) {
                TerraTool record = new TerraTool();
                record.setToolKey(toolKey);
                record.setName(annotation.name());
                record.setDescription(annotation.description());
                record.setSource("code");
                record.setExecSide("backend");
                record.setIsPreset(1);
                record.setIsEnabled(1);
                record.setCreateBy("system");
                toolMapper.insert(record);
            }
        } catch (Exception e) {
            log.warn("Failed to ensure code tool in DB: {}", toolKey, e);
        }
    }

    private Map<String, Object> buildParameterSchema(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        String[] paramNames = paramNameDiscoverer.getParameterNames(method);
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            String name = paramNames != null ? paramNames[i] : "arg" + i;
            properties.put(name, Map.of("type", javaTypeToJsonType(parameters[i].getType())));
        }
        schema.put("properties", properties);
        return schema;
    }

    private String javaTypeToJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == Integer.class || type == int.class
            || type == Long.class || type == long.class) return "integer";
        if (type == Double.class || type == double.class
            || type == Float.class || type == float.class) return "number";
        if (type == Boolean.class || type == boolean.class) return "boolean";
        return "object";
    }

    private Object[] mapParameters(Method method, Map<String, Object> params) {
        if (params == null) {
            params = Map.of();
        }
        String[] paramNames = paramNameDiscoverer.getParameterNames(method);
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            String name = paramNames != null ? paramNames[i] : "arg" + i;
            Object value = params.get(name);
            args[i] = convertValue(value, parameters[i].getType());
        }
        return args;
    }

    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        String str = value.toString();
        if (targetType == String.class) return str;
        if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(str);
        if (targetType == Long.class || targetType == long.class) return Long.valueOf(str);
        if (targetType == Double.class || targetType == double.class) return Double.valueOf(str);
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(str);
        return value;
    }
}
