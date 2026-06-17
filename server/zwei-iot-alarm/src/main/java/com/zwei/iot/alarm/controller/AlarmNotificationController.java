package com.zwei.iot.alarm.controller;

import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.utils.SecurityUtils;
import com.zwei.iot.alarm.domain.AlarmNotification;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationItemVO;
import com.zwei.iot.alarm.domain.dto.AlarmNotificationSummaryVO;
import com.zwei.iot.alarm.service.IAlarmNotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知中心（事件 Tab）API。
 * <p>
 * 用户视角：只看本人接收的 SYSTEM 渠道通知；标记已读仅对本人数据生效。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/alarm/notifications")
public class AlarmNotificationController extends BaseController {

    private static final String CHANNEL_SYSTEM = "SYSTEM";

    private final IAlarmNotificationService notificationService;

    public AlarmNotificationController(IAlarmNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 当前用户最近事件通知列表（默认 10 条，最多 100 条）。
     */
    @GetMapping("/recent")
    @PreAuthorize("@ss.hasPermi('alarm:notification:list')")
    public AjaxResult recent(@RequestParam(defaultValue = "10") int limit) {
        Long userId = SecurityUtils.getUserId();
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<AlarmNotification> list = notificationService.selectUserRecent(userId, safeLimit);
        List<AlarmNotificationItemVO> vos = list.stream()
            .map(this::toItemVO)
            .toList();
        return AjaxResult.success(vos);
    }

    /**
     * 当前用户未读事件数。
     */
    @GetMapping("/unread-count")
    @PreAuthorize("@ss.hasPermi('alarm:notification:list')")
    public AjaxResult unreadCount() {
        Long userId = SecurityUtils.getUserId();
        int count = notificationService.selectUnreadCount(userId, CHANNEL_SYSTEM);
        return AjaxResult.success(AlarmNotificationSummaryVO.builder()
            .unreadCount(count)
            .timestamp(System.currentTimeMillis())
            .build());
    }

    /**
     * 标记单条已读（仅当本人为接收人时生效）。
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("@ss.hasPermi('alarm:notification:read')")
    public AjaxResult read(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        int affected = notificationService.markReadIfOwner(id, userId);
        if (affected == 0) {
            return AjaxResult.error("通知不存在或无权操作");
        }
        return AjaxResult.success();
    }

    /**
     * 全部标记已读（当前用户 SYSTEM 渠道）。
     */
    @PostMapping("/read-all")
    @PreAuthorize("@ss.hasPermi('alarm:notification:read')")
    public AjaxResult readAll() {
        Long userId = SecurityUtils.getUserId();
        int affected = notificationService.markAllRead(userId, CHANNEL_SYSTEM);
        return AjaxResult.success("已标记 " + affected + " 条为已读");
    }

    private AlarmNotificationItemVO toItemVO(AlarmNotification n) {
        return AlarmNotificationItemVO.builder()
            .id(n.getId())
            .sourceType(n.getSourceType())
            .sourceId(n.getSourceId())
            .title(n.getTitle())
            .content(n.getContent())
            .recipientName(n.getRecipientName())
            .readTime(n.getReadTime())
            .createTime(n.getCreateTime())
            .build();
    }
}
