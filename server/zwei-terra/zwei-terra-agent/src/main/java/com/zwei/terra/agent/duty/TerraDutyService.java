package com.zwei.terra.agent.duty;

import com.zwei.terra.agent.chat.AnthropicChatModel;
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
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.*;

/**
 * 值守模式聊天编排服务 — 连接 Dashboard WebSocket 与 Terra AI 引擎。
 *
 * <p>核心职责：
 * <ol>
 *   <li>管理每个 WebSocket 连接的值守会话（conversation）</li>
 *   <li>将用户消息转为 ReAct Loop，调用 AnthropicChatModel</li>
 *   <li>将 LLM 输出翻译为 terramens WebSocket 协议事件（streaming timeline）</li>
 *   <li>执行后端工具（DashboardControlTool 等），结果通过 WebSocket 推送</li>
 * </ol>
 *
 * <p>与 {@link com.zwei.terra.agent.chat.ChatService} 的区别：
 * <ul>
 *   <li>输出目标是 WebSocket（而非 SSE）</li>
 *   <li>token 流转为 streaming timeline item（带 runId 分组）</li>
 *   <li>支持值守模式系统提示词（主动巡检 + 面板操控指导）</li>
 *   <li>同一连接的并发消息排队处理（避免 ReAct Loop 交错）</li>
 * </ul>
 */
@Service
@Slf4j
public class TerraDutyService {

    @Autowired private AnthropicChatModel chatModel;
    @Autowired private ToolManager toolManager;
    @Autowired private ITerraPersonalityService personalityService;
    @Autowired private TerraModelConfigMapper modelConfigMapper;
    @Autowired private TerraConversationMapper conversationMapper;
    @Autowired private TerraMessageMapper messageMapper;
    @Autowired private TerraProperties properties;
    @Autowired private TerraDutySessionManager sessionManager;

    /** 每个会话的对话队列 — 确保同一连接的消息串行处理 */
    private final Map<String, ExecutorService> sessionExecutors = new ConcurrentHashMap<>();

    /** 会话 → conversationId 映射 */
    private final Map<String, Long> sessionConversations = new ConcurrentHashMap<>();

    // ==================== 连接生命周期 ====================

    /**
     * 新值守连接建立 — 发送欢迎消息。
     */
    public void onConnect(WebSocketSession session) {
        // 发送欢迎时间线
        sessionManager.sendTo(session, DutyProtocol.timelineItem(
                "observation",
                "值守模式已启动。我正在监控所有设备状态和告警信息。",
                "terra"));

        // 更新 Terra 状态为巡检中
        sessionManager.sendTo(session, DutyProtocol.terraState("normal", "巡检中"));

        // 发送初始心跳
        sessionManager.sendTo(session, DutyProtocol.heartbeatTrigger());
    }

    /**
     * 连接断开 — 清理资源。
     */
    public void onDisconnect(WebSocketSession session) {
        String sessionId = session.getId();
        ExecutorService executor = sessionExecutors.remove(sessionId);
        if (executor != null) {
            executor.shutdownNow();
        }
        sessionConversations.remove(sessionId);
    }

    // ==================== 聊天入口 ====================

    /**
     * 处理用户聊天消息 — 异步执行 ReAct Loop。
     *
     * <p>同一连接的消息排队处理，确保不会交错。</p>
     *
     * @param session  WebSocket 会话
     * @param message  用户消息文本
     * @param userId   用户 ID
     */
    public void handleChat(WebSocketSession session, String message, Long userId) {
        String sessionId = session.getId();
        ExecutorService executor = sessionExecutors.computeIfAbsent(sessionId,
                k -> Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "duty-chat-" + sessionId);
                    t.setDaemon(true);
                    return t;
                }));

        executor.submit(() -> {
            try {
                executeDutyChat(session, message, userId);
            } catch (Exception e) {
                log.error("值守模式聊天异常: sessionId={}", sessionId, e);
                sessionManager.sendTo(session, DutyProtocol.timelineItem(
                        "warning", "处理消息时发生错误: " + e.getMessage(), "terra"));
            }
        });
    }

    // ==================== 值守 ReAct Loop ====================

    /**
     * 执行值守模式聊天 — 类似 ChatService.executeReactLoop 但输出到 WebSocket。
     */
    private void executeDutyChat(WebSocketSession session, String userMessage, Long userId) {
        String sessionId = session.getId();

        // 1. 添加用户消息到前端时间线
        sessionManager.sendTo(session, DutyProtocol.timelineItem(
                "observation", userMessage, "user"));

        // 2. 获取或创建会话
        Long conversationId = sessionConversations.get(sessionId);
        if (conversationId == null) {
            conversationId = createDutyConversation(userId, userMessage);
            sessionConversations.put(sessionId, conversationId);
        }

        // 3. 保存用户消息
        saveMessage(conversationId, "user", userMessage);

        // 4. 获取模型配置
        TerraModelConfig config = modelConfigMapper.selectActive();
        if (config == null) {
            sessionManager.sendTo(session, DutyProtocol.timelineItem(
                    "warning", "未找到激活的模型配置，请在管理后台设置。", "terra"));
            return;
        }

        // 5. 构建系统提示词
        String systemPrompt = buildDutySystemPrompt();

        // 6. 加载历史消息
        int historyLimit = properties.getChat().getMaxHistoryMessages();
        List<TerraMessage> history = messageMapper.selectByConversationId(conversationId, historyLimit);
        List<Map<String, Object>> messages = buildMessages(history);

        // 7. 获取可用工具
        List<ToolDefinition> tools = toolManager.getEnabledToolDefinitions();

        log.info("值守模式 ReAct 循环开始: convId={}, userId={}, tools={}",
                conversationId, userId, tools.size());

        // 8. 更新 Terra 状态为思考中
        sessionManager.sendTo(session, DutyProtocol.terraState("info", "正在分析..."));

        // 9. ReAct 循环
        int maxRounds = properties.getChat().getMaxReactRounds();

        for (int round = 0; round < maxRounds; round++) {
            // 生成 runId 用于流式消息分组
            String runId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

            // 收集工具调用
            List<AnthropicChatModel.ToolCallInfo> collectedToolCalls = new ArrayList<>();
            StringBuilder contentBuilder = new StringBuilder();
            boolean[] firstChunk = {true};

            // 调用 LLM
            AnthropicChatModel.AnthropicResponse response = chatModel.streamChat(
                    config,
                    systemPrompt,
                    messages,
                    tools,
                    token -> {
                        contentBuilder.append(token);
                        // 流式推送 — 第一次发送 isStreaming=true 的开始
                        sessionManager.sendTo(session, DutyProtocol.streamingTimelineItem(
                                contentBuilder.toString(), runId));
                        firstChunk[0] = false;
                    },
                    collectedToolCalls::add
            );

            // 判断停止原因
            if ("end_turn".equals(response.getStopReason()) && !response.hasToolCalls()) {
                // 对话结束 — 发送流式完成
                String finalContent = response.getContent() != null ? response.getContent() : "";
                sessionManager.sendTo(session, DutyProtocol.streamingCompleteTimelineItem(
                        finalContent, runId));

                // 保存 assistant 消息
                saveMessage(conversationId, "assistant", finalContent);

                // 恢复 Terra 状态为巡检中
                sessionManager.sendTo(session, DutyProtocol.terraState("normal", "巡检中"));

                log.info("值守模式 ReAct 循环正常结束: convId={}, rounds={}", conversationId, round + 1);
                return;
            }

            // 有工具调用
            if (response.hasToolCalls()) {
                // 发送流式完成（文本部分）
                if (!contentBuilder.isEmpty()) {
                    sessionManager.sendTo(session, DutyProtocol.streamingCompleteTimelineItem(
                            contentBuilder.toString(), runId));
                }

                // 将 assistant tool_use 消息追加
                messages.add(buildAssistantMessage(response.getContent(), response.getToolCalls()));

                // 处理每个工具调用
                for (AnthropicChatModel.ToolCallInfo toolCall : response.getToolCalls()) {
                    String callId = toolCall.getId();
                    String toolName = toolCall.getName();
                    Map<String, Object> params = toolCall.getInput();

                    // 推送工具调用时间线
                    sessionManager.sendTo(session, DutyProtocol.timelineItem(
                            "action", "正在执行: " + toolName, "terra"));

                    // 执行后端工具
                    ToolResult toolResult;
                    if (toolManager.isFrontendTool(toolName)) {
                        // 值守模式不支持前端工具（WebSocket 没有 SSE 回调机制）
                        toolResult = ToolResult.failure("值守模式不支持前端工具: " + toolName);
                    } else {
                        toolResult = executeBackendTool(toolName, params);
                    }

                    // 推送工具结果时间线
                    String resultSummary = toolResult.isSuccess()
                            ? truncateResult(toolResult.getResult())
                            : "执行失败: " + toolResult.getError();
                    sessionManager.sendTo(session, DutyProtocol.timelineItem(
                            "thinking", resultSummary, "terra"));

                    // 将 tool_result 追加到 messages
                    messages.add(buildToolResultMessage(callId, toolResult));
                }

                // 继续下一轮循环
                continue;
            }

            // stop_reason 非 end_turn 且无工具调用
            String reason = response.getStopReason() != null ? response.getStopReason() : "unknown";
            log.warn("值守模式 ReAct 循环异常停止: convId={}, stopReason={}", conversationId, reason);

            // 保存并发送
            String partialContent = response.getContent() != null ? response.getContent() : "";
            if (!partialContent.isEmpty()) {
                sessionManager.sendTo(session, DutyProtocol.streamingCompleteTimelineItem(
                        partialContent, runId));
                saveMessage(conversationId, "assistant", partialContent);
            }

            sessionManager.sendTo(session, DutyProtocol.terraState("normal", "巡检中"));
            return;
        }

        // 超过最大轮数
        sessionManager.sendTo(session, DutyProtocol.timelineItem(
                "warning", "对话轮数超过上限，请简化问题。", "terra"));
        sessionManager.sendTo(session, DutyProtocol.terraState("normal", "巡检中"));
    }

    // ==================== 系统提示词 ====================

    /**
     * 构建值守模式系统提示词。
     *
     * <p>在标准 Terra 人格基础上叠加值守模式指令。</p>
     */
    private String buildDutySystemPrompt() {
        StringBuilder sb = new StringBuilder();

        // 基础人格
        String basePrompt = personalityService.buildSystemPrompt();
        if (basePrompt != null && !basePrompt.isBlank()) {
            sb.append(basePrompt);
            sb.append("\n\n");
        }

        // 值守模式指令
        sb.append("""
                # 值守模式 (Duty Mode)

                你现在处于值守模式，正在通过实时监控仪表盘与操作员交互。这是你的核心职责：

                ## 能力
                1. **看板控制**：你可以使用 `dashboard` 系列工具创建和操控看板面板（图表、地图、表格、视频等）
                2. **系统查询**：你可以使用 `system.query` 系列工具查询知微系统的设备、隐患点、告警等实时数据
                3. **实时交互**：你的回复会实时显示在看板左侧的时间线上，操作员可以通过对话框与你交流

                ## 行为规范
                - 用户首次连接时，主动展示系统概况：使用 `system.query.overview` 获取数据，然后用 `dashboard` 工具创建面板展示
                - 当用户询问设备状态时，先查询数据，再用面板可视化展示
                - 当有告警时，使用 `dashboard.setTerraState` 更新头像状态，必要时用 `dashboard.showAlert` 弹出告警
                - 回复要简洁有力，避免冗长解释——值守人员需要快速获取关键信息
                - 创建面板时，合理选择面板类型和布局位置

                ## 面板类型指南
                - `createChart` — 趋势图、统计图（设备统计、告警趋势）
                - `createMap` — 地理分布图（隐患点分布、设备位置）
                - `createTable` — 列表数据（设备列表、告警列表）
                - `createVideo` — 视频监控画面
                - `createImage` — 图片展示
                - `createIframe` — 嵌入网页

                ## 工具调用提示
                - 调用工具前先简要说明你要做什么（会显示为 action 类型消息）
                - 工具返回后会显示结果摘要
                - 多次工具调用是正常的——你可以查询数据、创建面板、再查询更多数据
                """);

        return sb.toString();
    }

    // ==================== 工具执行 ====================

    private ToolResult executeBackendTool(String toolName, Map<String, Object> params) {
        try {
            log.debug("值守模式执行工具: tool={}, params={}", toolName, params);
            return toolManager.execute(toolName, params);
        } catch (Exception e) {
            log.error("值守模式工具执行异常: tool={}", toolName, e);
            return ToolResult.failure("工具执行异常: " + e.getMessage());
        }
    }

    private String truncateResult(Object result) {
        if (result == null) return "success";
        String str = result.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }

    // ==================== 会话与消息管理 ====================

    private Long createDutyConversation(Long userId, String firstMessage) {
        String title = "值守模式-" + (firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage);
        TerraConversation conversation = TerraConversation.builder()
                .userId(userId)
                .title(title)
                .status("active")
                .messageCount(0)
                .delFlag("0")
                .build();
        conversationMapper.insert(conversation);
        log.info("创建值守会话: id={}, userId={}, title={}", conversation.getId(), userId, title);
        return conversation.getId();
    }

    private void saveMessage(Long conversationId, String role, String content) {
        TerraMessage message = TerraMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .createTime(new Date())
                .build();
        messageMapper.insert(message);
        messageMapper.updateConversationStats(conversationId);
    }

    // ==================== Anthropic Messages 构建 ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildMessages(List<TerraMessage> history) {
        List<TerraMessage> ordered = new ArrayList<>(history);
        Collections.reverse(ordered);

        List<Map<String, Object>> messages = new ArrayList<>();
        for (TerraMessage msg : ordered) {
            if ("tool".equals(msg.getRole())) continue;
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("role", msg.getRole());
            message.put("content", msg.getContent() != null ? msg.getContent() : "");
            messages.add(message);
        }
        return messages;
    }

    private Map<String, Object> buildAssistantMessage(String textContent,
                                                       List<AnthropicChatModel.ToolCallInfo> toolCalls) {
        List<Map<String, Object>> contentBlocks = new ArrayList<>();

        if (textContent != null && !textContent.isBlank()) {
            Map<String, Object> textBlock = new LinkedHashMap<>();
            textBlock.put("type", "text");
            textBlock.put("text", textContent);
            contentBlocks.add(textBlock);
        }

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

    private Map<String, Object> buildToolResultMessage(String toolUseId, ToolResult result) {
        Map<String, Object> toolResultBlock = new LinkedHashMap<>();
        toolResultBlock.put("type", "tool_result");
        toolResultBlock.put("tool_use_id", toolUseId);

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
}
