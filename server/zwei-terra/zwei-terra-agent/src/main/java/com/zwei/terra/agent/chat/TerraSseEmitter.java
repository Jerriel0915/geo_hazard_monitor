package com.zwei.terra.agent.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terra SSE 事件封装 — 继承 Spring {@link SseEmitter}，提供面向 Terra 对话场景的便捷方法。
 *
 * <p>每个方法对应一种前端事件类型，所有方法内部 try-catch IOException，
 * 发送失败只记录日志不抛出异常（避免单个 SSE 事件失败中断整个对话流程）。</p>
 *
 * <p>事件类型一览：
 * <ul>
 *   <li>{@code token} — AI 文本增量（前端逐字渲染）</li>
 *   <li>{@code tool_call} — 工具调用请求（前端执行 frontend 工具）</li>
 *   <li>{@code tool_result} — 工具执行结果（后端工具执行完毕通知前端）</li>
 *   <li>{@code done} — 对话结束（含消息 ID 和 token 消耗）</li>
 *   <li>{@code error} — 错误通知</li>
 * </ul>
 *
 * @author zwei
 */
@Slf4j
public class TerraSseEmitter extends SseEmitter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TerraSseEmitter(long timeout) {
        super(timeout);
    }

    /**
     * 发送 token 事件 — AI 回复的增量文本。
     *
     * @param content 本次增量文本内容
     */
    public void sendToken(String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", content);
        sendEvent("token", data);
    }

    /**
     * 发送 tool_call 事件 — 通知前端执行工具调用。
     *
     * @param callId    工具调用 ID（用于关联请求与结果）
     * @param tool      工具名称
     * @param execSide  执行端（"backend" 或 "frontend"）
     * @param params    工具参数
     */
    public void sendToolCall(String callId, String tool, String execSide, Object params) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("callId", callId);
        data.put("tool", tool);
        data.put("execSide", execSide);
        data.put("params", params);
        sendEvent("tool_call", data);
    }

    /**
     * 发送 tool_result 事件 — 工具执行结果通知。
     *
     * @param callId  工具调用 ID（关联对应的 tool_call）
     * @param success 执行是否成功
     * @param result  执行结果数据（成功时为返回值，失败时为错误信息）
     */
    public void sendToolResult(String callId, boolean success, Object result) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("callId", callId);
        data.put("success", success);
        data.put("result", result);
        sendEvent("tool_result", data);
    }

    /**
     * 发送 done 事件 — 对话回合结束。
     *
     * @param messageId  对话消息 ID（数据库中的 terra_message.id）
     * @param tokensUsed 本轮消耗的 token 数（含 prompt + completion）
     */
    public void sendDone(Long messageId, int tokensUsed) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("messageId", messageId);
        data.put("tokensUsed", tokensUsed);
        sendEvent("done", data);
    }

    /**
     * 发送 error 事件 — 错误通知。
     *
     * @param message 错误描述
     */
    public void sendError(String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message);
        sendEvent("error", data);
    }

    // ==================== 内部方法 ====================

    /**
     * 统一发送 SSE 事件 — 序列化 data 为 JSON，失败只记日志不抛出。
     *
     * @param eventName SSE 事件名
     * @param data      事件数据对象（会被 ObjectMapper 序列化为 JSON）
     */
    private void sendEvent(String eventName, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            send(SseEmitter.event()
                    .name(eventName)
                    .data(jsonData));
        } catch (JsonProcessingException e) {
            log.error("SSE 事件序列化失败: event={}, data={}", eventName, data, e);
        } catch (IOException e) {
            log.warn("SSE 事件发送失败（客户端可能已断开）: event={}, error={}", eventName, e.getMessage());
        } catch (Exception e) {
            log.error("SSE 事件发送异常: event={}", eventName, e);
        }
    }
}
