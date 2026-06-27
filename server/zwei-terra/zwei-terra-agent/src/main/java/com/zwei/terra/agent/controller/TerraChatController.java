package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Terra 对话 Controller — SSE 流式对话端点 + 前端工具回调端点。
 *
 * <p>端点概览：
 * <ul>
 *   <li>{@code POST /api/v1/terra/chat} — 发起 SSE 流式对话</li>
 *   <li>{@code POST /api/v1/terra/chat/tool-result} — 前端工具执行结果回调</li>
 * </ul>
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/terra")
public class TerraChatController extends BaseController {

    @Autowired
    private ChatService chatService;

    /**
     * SSE 流式对话。
     *
     * <p>请求体 JSON：
     * <pre>{@code
     * {
     *   "conversationId": 123,   // 可选，首次对话不传则自动创建新会话
     *   "message": "用户输入"     // 必填
     * }
     * }</pre>
     *
     * <p>返回 SSE 事件流（Content-Type: text/event-stream）：
     * <ul>
     *   <li>{@code token} — AI 文本增量（前端逐字渲染）</li>
     *   <li>{@code tool_call} — 工具调用请求（前端执行 frontend 工具或展示后端执行状态）</li>
     *   <li>{@code tool_result} — 工具执行结果</li>
     *   <li>{@code done} — 对话结束（含消息 ID 和 token 消耗）</li>
     *   <li>{@code error} — 错误通知</li>
     * </ul>
     *
     * @param body 请求参数
     * @return SSE emitter
     */
    @PostMapping("/chat")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public SseEmitter chat(@RequestBody Map<String, Object> body) {
        Long conversationId = body.get("conversationId") != null
                ? Long.valueOf(body.get("conversationId").toString())
                : null;
        String message = body.get("message").toString();
        return chatService.chat(conversationId, message, getUserId());
    }

    /**
     * 前端工具执行结果回调。
     *
     * <p>当 SSE 收到 {@code tool_call(execSide=frontend)} 事件后，前端执行完毕，
     * 调用此端点提交结果，使 ReAct 循环继续。</p>
     *
     * <p>请求体 JSON：
     * <pre>{@code
     * {
     *   "callId": "toolu_xxx",   // SSE tool_call 事件中的 callId
     *   "success": true,         // 执行是否成功
     *   "result": { ... }         // 执行结果（成功）或错误信息（失败）
     * }
     * }</pre>
     *
     * @param body 回调参数
     * @return 操作结果
     */
    @PostMapping("/chat/tool-result")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult toolResult(@RequestBody Map<String, Object> body) {
        String callId = body.get("callId").toString();
        boolean success = Boolean.TRUE.equals(body.get("success"));
        Object result = body.get("result");
        chatService.resolveFrontendTool(callId, success, result);
        return success();
    }
}
