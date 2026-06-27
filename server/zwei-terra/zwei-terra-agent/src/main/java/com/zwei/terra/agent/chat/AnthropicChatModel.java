package com.zwei.terra.agent.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.terra.agent.domain.TerraModelConfig;
import com.zwei.terra.core.tool.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Anthropic Messages API HTTP 客户端 — 直接调用 /v1/messages 端点，支持 SSE 流式响应。
 *
 * <p>使用 Spring WebFlux {@link WebClient} 发起 POST 请求，通过 {@code bodyToFlux(String.class)}
 * 逐行解析 SSE 事件流，实时回调 tokenConsumer 与 toolCallConsumer。</p>
 *
 * <p>支持的 SSE 事件类型：
 * <ul>
 *   <li>{@code message_start} — 消息开始（含 message id/model）</li>
 *   <li>{@code content_block_start} — content block 开始（text 或 tool_use）</li>
 *   <li>{@code content_block_delta} — 增量内容（text_delta 或 input_json_delta）</li>
 *   <li>{@code content_block_stop} — content block 结束（tool_use 在此解析完整 input JSON）</li>
 *   <li>{@code message_delta} — 消息级更新（含 stop_reason）</li>
 *   <li>{@code message_stop} — 消息结束</li>
 * </ul>
 *
 * <p>设计决策：不使用 spring-ai-anthropic 封装库，因为需要支持自定义 base-url
 * （兼容 Anthropic API 兼容的第三方服务如 openrouter、本地模型等）。</p>
 *
 * @author zwei
 */
@Component
@Slf4j
public class AnthropicChatModel {

    private final ObjectMapper objectMapper;

    /**
     * 构造器注入 ObjectMapper。
     *
     * @param objectMapper Spring 容器提供的 Jackson ObjectMapper
     */
    public AnthropicChatModel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 发起流式对话请求，阻塞直到流结束。
     *
     * @param config            模型配置（baseUrl, apiKey, modelName, maxTokens, temperature）
     * @param systemPrompt      系统提示词（role=system），可为 null
     * @param messages          对话历史，每条消息为 {role, content} 结构
     * @param tools             可用工具定义列表，可为空（表示不启用工具调用）
     * @param tokenConsumer     文本 token 回调，每收到一个 text_delta 调用一次
     * @param toolCallConsumer  工具调用回调，当 content_block_stop 且为 tool_use 时调用
     * @return {@link AnthropicResponse} 包含完整文本内容、stop_reason 和工具调用列表
     * @throws RuntimeException 当 HTTP 请求失败或 SSE 流解析出错时抛出
     */
    public AnthropicResponse streamChat(
            TerraModelConfig config,
            String systemPrompt,
            List<Map<String, Object>> messages,
            List<ToolDefinition> tools,
            Consumer<String> tokenConsumer,
            Consumer<ToolCallInfo> toolCallConsumer) {

        // 构建请求体
        Map<String, Object> requestBody = buildRequestBody(config, systemPrompt, messages, tools);
        String url = config.getBaseUrl() + "/v1/messages";

        log.debug("发起 Anthropic 流式请求: url={}, model={}, messages={}", url, config.getModelName(), messages.size());

        // 累积响应内容
        StringBuilder contentBuilder = new StringBuilder();
        List<ToolCallInfo> toolCalls = new ArrayList<>();
        String[] stopReasonHolder = {null};

        // content block 跟踪状态
        // key=blockIndex, value=block 状态对象
        Map<Integer, BlockState> blockStates = new LinkedHashMap<>();

        WebClient client = WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("x-api-key", config.getApiKey())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();

        try {
            client.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(line -> {
                        try {
                            parseSseLine(line, contentBuilder, toolCalls, stopReasonHolder,
                                    blockStates, tokenConsumer, toolCallConsumer);
                        } catch (Exception e) {
                            log.error("SSE 事件解析异常: line={}", line, e);
                        }
                    })
                    .blockLast();
        } catch (Exception e) {
            log.error("Anthropic 流式请求失败: url={}", url, e);
            throw new RuntimeException("Anthropic API 请求失败: " + e.getMessage(), e);
        }

        AnthropicResponse response = new AnthropicResponse();
        response.setContent(contentBuilder.toString());
        response.setStopReason(stopReasonHolder[0]);
        response.setToolCalls(toolCalls);

        log.info("Anthropic 流式请求完成: model={}, contentLength={}, toolCalls={}, stopReason={}",
                config.getModelName(), response.getContent().length(), toolCalls.size(), response.getStopReason());

        return response;
    }

    // ==================== SSE 事件解析 ====================

    /**
     * 解析单行 SSE 事件数据。
     *
     * <p>WebClient bodyToFlux(String.class) 会将 SSE 数据行逐行传递（已去掉 "data: " 前缀）。
     * 每行是一个 JSON 对象，格式为 {@code {"type":"...", ...}}。</p>
     */
    @SuppressWarnings("unchecked")
    private void parseSseLine(
            String line,
            StringBuilder contentBuilder,
            List<ToolCallInfo> toolCalls,
            String[] stopReasonHolder,
            Map<Integer, BlockState> blockStates,
            Consumer<String> tokenConsumer,
            Consumer<ToolCallInfo> toolCallConsumer) throws Exception {

        if (line == null || line.isBlank()) {
            return;
        }

        Map<String, Object> event = objectMapper.readValue(line, Map.class);
        String type = (String) event.get("type");

        if (type == null) {
            return;
        }

        switch (type) {
            case "message_start" -> {
                log.debug("SSE: message_start");
            }
            case "content_block_start" -> {
                handleContentBlockStart(event, blockStates);
            }
            case "content_block_delta" -> {
                handleContentBlockDelta(event, contentBuilder, blockStates, tokenConsumer);
            }
            case "content_block_stop" -> {
                handleContentBlockStop(event, blockStates, toolCalls, toolCallConsumer);
            }
            case "message_delta" -> {
                handleMessageDelta(event, stopReasonHolder);
            }
            case "message_stop" -> {
                log.debug("SSE: message_stop");
            }
            case "ping" -> {
                log.trace("SSE: ping");
            }
            case "error" -> {
                Map<String, Object> error = (Map<String, Object>) event.get("error");
                String errorMsg = error != null ? (String) error.get("message") : "unknown error";
                log.error("SSE: error event — {}", errorMsg);
                throw new RuntimeException("Anthropic API 返回错误: " + errorMsg);
            }
            default -> log.debug("SSE: 未处理的事件类型 type={}", type);
        }
    }

    /**
     * 处理 content_block_start 事件 — 标记 content block 开始。
     *
     * <p>如果是 tool_use 类型，保存 id 和 name，后续 input_json_delta 会累积到此 block。</p>
     */
    @SuppressWarnings("unchecked")
    private void handleContentBlockStart(Map<String, Object> event, Map<Integer, BlockState> blockStates) {
        Integer index = (Integer) event.get("index");
        Map<String, Object> contentBlock = (Map<String, Object>) event.get("content_block");
        if (contentBlock == null) {
            return;
        }

        String blockType = (String) contentBlock.get("type");
        BlockState state = new BlockState();
        state.type = blockType;

        if ("tool_use".equals(blockType)) {
            state.toolId = (String) contentBlock.get("id");
            state.toolName = (String) contentBlock.get("name");
            state.inputJsonBuilder = new StringBuilder();
            log.debug("SSE: content_block_start [tool_use] index={}, id={}, name={}", index, state.toolId, state.toolName);
        } else {
            log.debug("SSE: content_block_start [{}] index={}", blockType, index);
        }

        blockStates.put(index, state);
    }

    /**
     * 处理 content_block_delta 事件 — 增量内容。
     *
     * <p>text_delta → 累积到 contentBuilder 并回调 tokenConsumer；
     * input_json_delta → 累积到对应 block 的 StringBuilder。</p>
     */
    @SuppressWarnings("unchecked")
    private void handleContentBlockDelta(
            Map<String, Object> event,
            StringBuilder contentBuilder,
            Map<Integer, BlockState> blockStates,
            Consumer<String> tokenConsumer) {

        Integer index = (Integer) event.get("index");
        Map<String, Object> delta = (Map<String, Object>) event.get("delta");
        if (delta == null) {
            return;
        }

        String deltaType = (String) delta.get("type");

        if ("text_delta".equals(deltaType)) {
            String text = (String) delta.get("text");
            if (text != null && !text.isEmpty()) {
                contentBuilder.append(text);
                if (tokenConsumer != null) {
                    tokenConsumer.accept(text);
                }
            }
        } else if ("input_json_delta".equals(deltaType)) {
            // 累积工具调用参数的 JSON 片段
            String partialJson = (String) delta.get("partial_json");
            BlockState state = blockStates.get(index);
            if (state != null && state.inputJsonBuilder != null && partialJson != null) {
                state.inputJsonBuilder.append(partialJson);
            }
        }
    }

    /**
     * 处理 content_block_stop 事件 — content block 结束。
     *
     * <p>如果是 tool_use 类型的 block，解析累积的 JSON 为 Map，构建 ToolCallInfo 并回调。</p>
     */
    @SuppressWarnings("unchecked")
    private void handleContentBlockStop(
            Map<String, Object> event,
            Map<Integer, BlockState> blockStates,
            List<ToolCallInfo> toolCalls,
            Consumer<ToolCallInfo> toolCallConsumer) {

        Integer index = (Integer) event.get("index");
        BlockState state = blockStates.remove(index);
        if (state == null || !"tool_use".equals(state.type)) {
            return;
        }

        // 解析累积的 input JSON
        Map<String, Object> input = Map.of();
        if (state.inputJsonBuilder != null && !state.isEmpty()) {
            String jsonStr = state.inputJsonBuilder.toString().trim();
            if (!jsonStr.isEmpty()) {
                try {
                    input = objectMapper.readValue(jsonStr, Map.class);
                } catch (Exception e) {
                    log.error("工具调用 input JSON 解析失败: toolName={}, json={}", state.toolName, jsonStr, e);
                    input = Map.of("_parse_error", jsonStr);
                }
            }
        }

        ToolCallInfo toolCall = new ToolCallInfo();
        toolCall.setId(state.toolId);
        toolCall.setName(state.toolName);
        toolCall.setInput(input);

        toolCalls.add(toolCall);
        log.debug("SSE: content_block_stop [tool_use] 解析完成: name={}, params={}", state.toolName, input);

        if (toolCallConsumer != null) {
            toolCallConsumer.accept(toolCall);
        }
    }

    /**
     * 处理 message_delta 事件 — 提取 stop_reason。
     */
    @SuppressWarnings("unchecked")
    private void handleMessageDelta(Map<String, Object> event, String[] stopReasonHolder) {
        Map<String, Object> delta = (Map<String, Object>) event.get("delta");
        if (delta != null && delta.get("stop_reason") != null) {
            stopReasonHolder[0] = (String) delta.get("stop_reason");
            log.debug("SSE: message_delta stop_reason={}", stopReasonHolder[0]);
        }
    }

    // ==================== 请求体构建 ====================

    /**
     * 构建 Anthropic Messages API 请求体。
     *
     * <p>请求体结构：
     * <pre>{@code
     * {
     *   "model": "claude-sonnet-4-...",
     *   "max_tokens": 4096,
     *   "temperature": 0.7,
     *   "stream": true,
     *   "system": "...",          // 可选
     *   "messages": [...],
     *   "tools": [...]            // 可选
     * }
     * }</pre>
     */
    private Map<String, Object> buildRequestBody(
            TerraModelConfig config,
            String systemPrompt,
            List<Map<String, Object>> messages,
            List<ToolDefinition> tools) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("max_tokens", config.getMaxTokens() != null ? config.getMaxTokens() : 4096);
        body.put("temperature", config.getTemperature() != null ? config.getTemperature().doubleValue() : 0.7);
        body.put("stream", true);

        // 系统提示词（Anthropic 使用顶层 system 字段，不是 messages 中的 role=system）
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }

        body.put("messages", messages != null ? messages : List.of());

        // 工具定义
        if (tools != null && !tools.isEmpty()) {
            List<Map<String, Object>> toolDefs = new ArrayList<>();
            for (ToolDefinition tool : tools) {
                Map<String, Object> toolDef = new LinkedHashMap<>();
                toolDef.put("name", tool.getName());
                toolDef.put("description", tool.getDescription());
                // input_schema 即工具参数 schema
                Map<String, Object> inputSchema = tool.getParametersSchema();
                if (inputSchema == null) {
                    inputSchema = new LinkedHashMap<>();
                    inputSchema.put("type", "object");
                    inputSchema.put("properties", Map.of());
                }
                toolDef.put("input_schema", inputSchema);
                toolDefs.add(toolDef);
            }
            body.put("tools", toolDefs);
        }

        return body;
    }

    // ==================== 内部类 ====================

    /**
     * content block 跟踪状态 — 在 SSE 流中逐 block 记录类型和累积数据。
     */
    private static class BlockState {
        String type;             // "text" | "tool_use"
        String toolId;           // tool_use 时保存 tool_use_id
        String toolName;         // tool_use 时保存工具名
        StringBuilder inputJsonBuilder;  // tool_use 时累积 input_json_delta 片段

        boolean isEmpty() {
            return inputJsonBuilder == null || inputJsonBuilder.length() == 0;
        }
    }

    /**
     * 工具调用信息 — 从 SSE 流中解析出的单个工具调用。
     */
    @lombok.Getter
    @lombok.Setter
    public static class ToolCallInfo {
        /** Anthropic 分配的工具调用 ID（用于关联 tool_result） */
        private String id;
        /** 工具名称 */
        private String name;
        /** 工具输入参数（已解析为 Map） */
        private Map<String, Object> input;
    }

    /**
     * 流式对话响应 — 包含完整文本、停止原因和工具调用列表。
     */
    @lombok.Getter
    @lombok.Setter
    public static class AnthropicResponse {
        /** 完整文本内容（所有 text_delta 拼接） */
        private String content;
        /** 停止原因（end_turn / tool_use / max_tokens / stop_sequence） */
        private String stopReason;
        /** 工具调用列表（stopReason 为 tool_use 时非空） */
        private List<ToolCallInfo> toolCalls;

        /**
         * 是否需要执行工具调用。
         */
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
