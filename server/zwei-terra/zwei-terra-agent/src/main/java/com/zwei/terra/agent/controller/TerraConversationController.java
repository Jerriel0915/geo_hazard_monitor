package com.zwei.terra.agent.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.terra.agent.domain.TerraConversation;
import com.zwei.terra.agent.mapper.TerraConversationMapper;
import com.zwei.terra.agent.mapper.TerraMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Terra 会话管理 Controller — 会话列表、消息历史、新建、删除。
 *
 * <p>端点概览：
 * <ul>
 *   <li>{@code GET  /api/v1/terra/conversations} — 当前用户会话列表</li>
 *   <li>{@code GET  /api/v1/terra/conversations/{id}/messages} — 会话消息历史</li>
 *   <li>{@code POST /api/v1/terra/conversations} — 新建会话</li>
 *   <li>{@code DELETE /api/v1/terra/conversations/{id}} — 逻辑删除会话</li>
 * </ul>
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/terra/conversations")
public class TerraConversationController extends BaseController {

    @Autowired
    private TerraConversationMapper conversationMapper;

    @Autowired
    private TerraMessageMapper messageMapper;

    /**
     * 查询当前用户的会话列表。
     *
     * @return 会话列表（按最后消息时间倒序）
     */
    @GetMapping
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult list() {
        List<TerraConversation> list = conversationMapper.selectByUserId(getUserId());
        return success(list);
    }

    /**
     * 查询会话消息历史。
     *
     * @param id 会话 ID
     * @return 消息列表（最多 100 条，按时间正序）
     */
    @GetMapping("/{id}/messages")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult messages(@PathVariable Long id) {
        // 验证会话归属权
        TerraConversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            return error("会话不存在");
        }
        if (!getUserId().equals(conversation.getUserId())) {
            return error("无权访问此会话");
        }
        return success(messageMapper.selectByConversationId(id, 100));
    }

    /**
     * 新建会话。
     *
     * <p>请求体 JSON：{@code {"title": "会话标题"}}</p>
     *
     * @param body 请求参数
     * @return 新建的会话信息
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult create(@RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? body.get("title").toString() : "新对话";

        TerraConversation conversation = TerraConversation.builder()
                .userId(getUserId())
                .title(title)
                .status("active")
                .messageCount(0)
                .delFlag("0")
                .build();
        conversationMapper.insert(conversation);
        return success(conversation);
    }

    /**
     * 逻辑删除会话。
     *
     * @param id 会话 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('terra:chat')")
    public AjaxResult delete(@PathVariable Long id) {
        // 验证会话归属权
        TerraConversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            return error("会话不存在");
        }
        if (!getUserId().equals(conversation.getUserId())) {
            return error("无权删除此会话");
        }
        return toAjax(conversationMapper.deleteById(id));
    }
}
