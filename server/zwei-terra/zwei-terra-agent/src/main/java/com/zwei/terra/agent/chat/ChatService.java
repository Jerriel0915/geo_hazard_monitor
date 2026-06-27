package com.zwei.terra.agent.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.terra.agent.config.TerraProperties;
import com.zwei.terra.agent.domain.TerraConversation;
import com.zwei.terra.agent.domain.TerraMessage;
import com.zwei.terra.agent.domain.TerraModelConfig;
import com.zwei.terra.agent.mapper.TerraConversationMapper;
import com.zwei.terra.agent.mapper.TerraMessageMapper;
import com.zwei.terra.agent.mapper.TerraModelConfigMapper;
import com.zwei.terra.agent.service.ITerraPersonalityService;
import com.zwei.terra.agent.tool.ToolManager;
import com.zwei.terra.core.tool.ToolDefinition;
import com.zwei.terra.core.tool.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.*;

/**
 * 对话编排服务 — 核心 ReAct (Reasoning + Acting) Loop 引擎。
 *
 * <p>职责链：
 * <ol>
 *   <li>创建/验证会话，持久化用户消息</li>
 *   <li>加载模型配置、系统提示词、历史消息、可用工具</li>
 *   <li>ReAct 循环：调用 LLM → 判断 stop_reason → 执行工具（前端/后端）→ 追加结果 → 继续循环</li>
 *   <li>通过 {@link TerraSseEmitter} 实时推送 token / tool_call / tool_result / done 事件</li>
 * </ol>
 *
 * <p>前端工具执行流程：SSE 推送 tool_call(frontend) → 前端执行完毕 POST /chat/tool-result →
 * {@link #resolveFrontendTool} 完成 CompletableFuture → 循环继续。</p>
 *
 * @author zwei
 */
@Service
@Slf4j
public class ChatService {

    @Autowired private AnthropicChatModel chatModel;
    @Autowired private ToolManager toolManager;
    @Autowired private ITerraPersonalityService personalityService;
    @Autowired private TerraModelConfigMapper modelConfigMapper;
    @Autowired private TerraConversationMapper conversationMapper;
    @Autowired private TerraMessageMapper messageMapper;
    @Autowired private TerraProperties properties;
    @Autowired private ObjectMapper objectMapper;

    /** 前端工具调用挂起表: callId → CompletableFuture，等待前端回调 */
    private final Map<String, CompletableFuture<ToolResult>> pendingFrontendTools = new ConcurrentHashMap<>();

    /** SSE 连接超时时间（5 分钟） */
    private static final long SSE_TIMEOUT = 300_000L;

    // ==================== 公开入口 ====================

    /**
     * 发起对话 — 返回 SSE emitter，ReAct 循环异步执行。
     *
     * @param conversationId 会话 ID（首次对话可为 null，自动创建新会话）
     * @param userMessage    用户输入文本
     * @param userId         当前登录用户 ID
     * @return SSE emitter，前端通过 EventSource 接收流式事件
     */
    public SseEmitter chat(Long conversationId, String userMessage, Long userId) {
        TerraSseEmitter emitter = new TerraSseEmitter(SSE_TIMEOUT, objectMapper);

        CompletableFuture.runAsync(() -> {
            try {
                executeReactLoop(emitter, conversationId, userMessage, userId);
            } catch (Exception e) {
                log.error("ReAct 循环异常: conversationId={}, userId={}", conversationId, userId, e);
                emitter.sendError("对话处理异常: " + e.getMessage());
            } finally {
                try {
                    emitter.complete();
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    /**
     * 前端工具执行结果回调 — 由 Controller 调用，解除对应 CompletableFuture 的阻塞。
     *
     * @param callId  工具调用 ID（与 SSE tool_call 事件中的 callId 一致）
     * @param success 前端执行是否成功
     * @param result  执行结果数据（成功为返回值，失败为错误信息）
     */
    public void resolveFrontendTool(String callId, boolean success, Object result) {
        CompletableFuture<ToolResult> future = pendingFrontendTools.remove(callId);
        if (future == null) {
            log.warn("前端工具回调未找到挂起任务: callId={}", callId);
            return;
        }
        ToolResult toolResult = success
                ? ToolResult.success(result)
                : ToolResult.failure(result != null ? result.toString() : "前端工具执行失败");
        future.complete(toolResult);
        log.debug("前端工具回调完成: callId={}, success={}", callId, success);
    }

    // ==================== ReAct Loop ====================

    /**
     * 执行 ReAct 循环核心逻辑。
     *
     * <p>循环流程（最多 {@code terra.chat.max-react-rounds} 轮）：
     * <pre>
     *   1. 调用 LLM streamChat（流式推送 token）
     *   2. 若 stop_reason = end_turn → 保存 assistant 消息，推送 done，结束
     *   3. 若有 tool_calls → 逐个执行（前端/后端），推送 tool_call + tool_result
     *   4. 将 assistant tool_use + tool_result 追加到 messages，继续循环
     * </pre>
     */
    private void executeReactLoop(TerraSseEmitter emitter, Long conversationId,
                                  String userMessage, Long userId) {
        // 1. 会话管理
        TerraConversation conversation = getOrCreateConversation(conversationId, userId, userMessage);
        Long convId = conversation.getId();

        // 2. 保存用户消息
        saveMessage(convId, "user", userMessage, null, null);

        // 3. 获取激活的模型配置
        TerraModelConfig config = modelConfigMapper.selectActive();
        if (config == null) {
            emitter.sendError("未找到激活的模型配置，请在管理后台设置");
            return;
        }

        // 4. 构建系统提示词
        String systemPrompt = personalityService.buildSystemPrompt();

        // 5. 加载历史消息
        int historyLimit = properties.getChat().getMaxHistoryMessages();
        List<TerraMessage> history = messageMapper.selectByConversationId(convId, historyLimit);

        // 6. 构建 messages 列表（Anthropic Messages API 格式）
        List<Map<String, Object>> messages = buildMessages(history);

        // 7. 获取可用工具
        List<ToolDefinition> tools = toolManager.getEnabledToolDefinitions();

        log.info("ReAct 循环开始: convId={}, userId={}, history={}, tools={}",
                convId, userId, messages.size(), tools.size());

        // 8. ReAct 循环
        int maxRounds = properties.getChat().getMaxReactRounds();
        int totalTokensUsed = 0;

        for (int round = 0; round < maxRounds; round++) {
            log.debug("ReAct 第 {} 轮: convId={}", round + 1, convId);

            // 收集本轮 LLM 返回的工具调用（streamChat 在同一线程同步回调）
            List<AnthropicChatModel.ToolCallInfo> collectedToolCalls = new ArrayList<>();

            // 调用 LLM
            AnthropicChatModel.AnthropicResponse response = chatModel.streamChat(
                    config,
                    systemPrompt,
                    messages,
                    tools,
                    emitter::sendToken,
                    collectedToolCalls::add
            );

            if (response.getContent() != null && !response.getContent().isEmpty()) {
                totalTokensUsed += estimateTokens(response.getContent());
            }

            // 判断停止原因
            if ("end_turn".equals(response.getStopReason()) && !response.hasToolCalls()) {
                // 对话结束 — 保存 assistant 消息
                TerraMessage assistantMsg = saveMessage(convId, "assistant",
                        response.getContent(), null, null);
                emitter.sendDone(assistantMsg.getId(), totalTokensUsed);
                log.info("ReAct 循环正常结束: convId={}, rounds={}, tokens={}",
                        convId, round + 1, totalTokensUsed);
                return;
            }

            // 有工具调用 — 处理每个工具
            if (response.hasToolCalls()) {
                // 将 assistant 的 tool_use 消息追加到 messages
                // 如果同时有文本内容和工具调用，需要构建包含文本 + tool_use 的复合消息
                messages.add(buildAssistantMessage(response.getContent(), response.getToolCalls()));

                for (AnthropicChatModel.ToolCallInfo toolCall : response.getToolCalls()) {
                    String callId = toolCall.getId();
                    String toolName = toolCall.getName();
                    Map<String, Object> params = toolCall.getInput();

                    ToolResult toolResult;
                    if (toolManager.isFrontendTool(toolName)) {
                        // 前端工具：SSE 推送 → 等待回调
                        emitter.sendToolCall(callId, toolName, "frontend", params);
                        toolResult = waitForFrontendTool(callId, toolName);
                    } else {
                        // 后端工具：SSE 推送 → 直接执行
                        emitter.sendToolCall(callId, toolName, "backend", params);
                        toolResult = executeBackendTool(toolName, params);
                    }

                    // 推送工具执行结果
                    emitter.sendToolResult(callId, toolResult.isSuccess(),
                            toolResult.isSuccess() ? toolResult.getResult() : toolResult.getError());

                    // 将 tool_result 追加到 messages
                    messages.add(buildToolResultMessage(callId, toolResult));
                }

                // 继续下一轮循环
                continue;
            }

            // stop_reason 非 end_turn 且无工具调用（如 max_tokens）
            String reason = response.getStopReason() != null ? response.getStopReason() : "unknown";
            log.warn("ReAct 循环异常停止: convId={}, stopReason={}", convId, reason);
            TerraMessage assistantMsg = saveMessage(convId, "assistant",
                    response.getContent(), null, null);
            emitter.sendDone(assistantMsg.getId(), totalTokensUsed);
            return;
        }

        // 超过最大轮数
        log.warn("ReAct 循环达到最大轮数: convId={}, maxRounds={}", convId, maxRounds);
        emitter.sendError("对话轮数超过上限 (" + maxRounds + ")，请简化问题或稍后重试");
    }

    // ==================== 工具执行 ====================

    /**
     * 等待前端工具回调完成。
     *
     * <p>通过 CompletableFuture + ConcurrentHashMap 实现异步等待：
     * 前端执行完毕后调用 POST /chat/tool-result，触发 {@link #resolveFrontendTool}。</p>
     *
     * @param callId    工具调用 ID
     * @param toolName  工具名称（用于日志和超时消息）
     * @return 工具执行结果
     */
    private ToolResult waitForFrontendTool(String callId, String toolName) {
        CompletableFuture<ToolResult> future = new CompletableFuture<>();
        pendingFrontendTools.put(callId, future);

        int timeout = properties.getChat().getDefaultTimeoutSeconds();
        try {
            log.debug("等待前端工具回调: callId={}, tool={}, timeout={}s", callId, toolName, timeout);
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingFrontendTools.remove(callId);
            log.warn("前端工具执行超时: callId={}, tool={}, timeout={}s", callId, toolName, timeout);
            return ToolResult.failure("前端工具执行超时: " + toolName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pendingFrontendTools.remove(callId);
            return ToolResult.failure("前端工具执行被中断: " + toolName);
        } catch (ExecutionException e) {
            pendingFrontendTools.remove(callId);
            log.error("前端工具执行异常: callId={}, tool={}", callId, toolName, e);
            return ToolResult.failure("前端工具执行异常: " + e.getMessage());
        }
    }

    /**
     * 执行后端工具（带异常保护）。
     *
     * @param toolName 工具名称（toolKey）
     * @param params   工具参数
     * @return 工具执行结果
     */
    private ToolResult executeBackendTool(String toolName, Map<String, Object> params) {
        try {
            log.debug("执行后端工具: tool={}, params={}", toolName, params);
            return toolManager.execute(toolName, params);
        } catch (Exception e) {
            log.error("后端工具执行异常: tool={}", toolName, e);
            return ToolResult.failure("工具执行异常: " + e.getMessage());
        }
    }

    // ==================== 会话与消息管理 ====================

    /**
     * 获取或创建会话。
     *
     * <p>conversationId 为 null 时创建新会话（标题取用户消息前 30 字符）；
     * 否则验证会话存在且属于当前用户。</p>
     *
     * @param conversationId 会话 ID（可为 null）
     * @param userId         用户 ID
     * @param userMessage    用户消息（用于自动生成标题）
     * @return 会话实体
     */
    private TerraConversation getOrCreateConversation(Long conversationId, Long userId, String userMessage) {
        if (conversationId != null) {
            TerraConversation existing = conversationMapper.selectById(conversationId);
            if (existing == null) {
                throw new IllegalStateException("会话不存在: " + conversationId);
            }
            if (!userId.equals(existing.getUserId())) {
                throw new SecurityException("无权访问此会话: " + conversationId);
            }
            return existing;
        }

        // 创建新会话
        String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "..." : userMessage;
        TerraConversation conversation = TerraConversation.builder()
                .userId(userId)
                .title(title)
                .status("active")
                .messageCount(0)
                .delFlag("0")
                .build();
        conversationMapper.insert(conversation);
        log.info("创建新会话: id={}, userId={}, title={}", conversation.getId(), userId, title);
        return conversation;
    }

    /**
     * 保存消息并更新会话统计。
     *
     * @param conversationId 会话 ID
     * @param role           角色（user / assistant / tool）
     * @param content        消息内容
     * @param toolCalls      工具调用 JSON（可为 null）
     * @param toolCallId     工具调用关联 ID（可为 null）
     * @return 已插入的消息实体（含自增 ID）
     */
    private TerraMessage saveMessage(Long conversationId, String role, String content,
                                     String toolCalls, String toolCallId) {
        TerraMessage message = TerraMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .toolCalls(toolCalls)
                .toolCallId(toolCallId)
                .createTime(new Date())
                .build();
        messageMapper.insert(message);
        messageMapper.updateConversationStats(conversationId);
        return message;
    }

    // ==================== Anthropic Messages 构建 ====================

    /**
     * 将历史消息转为 Anthropic Messages API 格式。
     *
     * <p>每条消息格式：{@code {"role": "user"|"assistant", "content": "..."}}</p>
     *
     * @param history 历史消息列表
     * @return Anthropic API 格式的消息列表
     */
    private List<Map<String, Object>> buildMessages(List<TerraMessage> history) {
        List<Map<String, Object>> messages = new ArrayList<>();
        for (TerraMessage msg : history) {
            // 跳过 tool 角色消息（Anthropic 用 tool_result content block 而非独立消息角色）
            if ("tool".equals(msg.getRole())) {
                continue;
            }
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", msg.getRole());
            message.put("content", msg.getContent() != null ? msg.getContent() : "");
            messages.add(message);
        }
        return messages;
    }

    /**
     * 构建 assistant 消息（包含文本和工具调用）。
     *
     * <p>Anthropic 格式要求 assistant 消息的 content 为数组，可同时包含 text 和 tool_use block：
     * <pre>{@code
     * {
     *   "role": "assistant",
     *   "content": [
     *     {"type": "text", "text": "..."},
     *     {"type": "tool_use", "id": "...", "name": "...", "input": {...}}
     *   ]
     * }
     * }</pre>
     *
     * @param textContent 文本内容（可为 null 或空）
     * @param toolCalls   工具调用列表
     * @return Anthropic 格式的 assistant 消息
     */
    private Map<String, Object> buildAssistantMessage(String textContent,
                                                       List<AnthropicChatModel.ToolCallInfo> toolCalls) {
        List<Map<String, Object>> contentBlocks = new ArrayList<>();

        // 文本内容 block
        if (textContent != null && !textContent.isBlank()) {
            Map<String, Object> textBlock = new LinkedHashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", textContent);
            contentBlocks.add(textBlock);
        }

        // 工具调用 blocks
        for (AnthropicChatModel.ToolCallInfo toolCall : toolCalls) {
            Map<String, Object> toolUseBlock = new LinkedHashMap<>();
            toolUseBlock.put("type", "tool_use");
            toolUseBlock.put("id", toolCall.getId());
            toolUseBlock.put("name", toolCall.getName());
            toolUseBlock.put("input", toolCall.getInput() != null ? toolCall.getInput() : Map.of());
            contentBlocks.add(toolUseBlock);
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", contentBlocks);
        return message;
    }

    /**
     * 构建 tool_result 消息（作为 user 角色发送给 Anthropic）。
     *
     * <p>Anthropic 格式：
     * <pre>{@code
     * {
     *   "role": "user",
     *   "content": [
     *     {
     *       "type": "tool_result",
     *       "tool_use_id": "...",
     *       "content": "结果文本"
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @param toolUseId 关联的 tool_use ID
     * @param result    工具执行结果
     * @return Anthropic 格式的 tool_result 消息
     */
    private Map<String, Object> buildToolResultMessage(String toolUseId, ToolResult result) {
        Map<String, Object> toolResultBlock = new LinkedHashMap<>();
        toolResultBlock.put("type", "tool_result");
        toolResultBlock.put("tool_use_id", toolUseId);

        // 结果内容
        String resultContent;
        if (result.isSuccess()) {
            resultContent = result.getResult() != null ? result.getResult().toString() : "success";
        } else {
            resultContent = result.getError() != null ? result.getError() : "execution failed";
        }
        toolResultBlock.put("content", resultContent);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", List.of(toolResultBlock));
        return message;
    }

    // ==================== 工具方法 ====================

    /**
     * 粗略估算 token 用量（4 字符 ≈ 1 token）。
     *
     * <p>仅用于 done 事件的参考值，实际用量以 API 返回为准。</p>
     *
     * @param text 文本内容
     * @return 估算的 token 数
     */
    private int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.length() / 4;
    }
}
