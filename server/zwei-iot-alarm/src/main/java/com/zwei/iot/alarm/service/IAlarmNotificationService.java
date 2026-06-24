package com.zwei.iot.alarm.service;

import com.zwei.iot.alarm.domain.AlarmNotification;

import java.util.List;

/**
 * 告警通知服务接口
 *
 * @author zwei
 */
public interface IAlarmNotificationService {

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    List<AlarmNotification> selectList(AlarmNotification notification);

    int createNotification(AlarmNotification notification);

    int batchCreate(List<AlarmNotification> notifications);

    int updateStatus(Long id, Integer status, String errorMsg);

    /**
     * 标记通知为已发送
     *
     * @param id 通知 ID
     */
    void markSent(Long id);

    /**
     * 标记通知为发送失败，根据错误码推导最终 status
     *
     * @param id               通知 ID
     * @param errorCode        错误码 (如 RECIPIENT_PHONE_MISSING)
     * @param errorDescription 错误描述
     */
    void markFailed(Long id, String errorCode, String errorDescription);

    /**
     * 仅当 userId 为该通知接收人时，标记为已读
     *
     * @param id     通知 ID
     * @param userId 当前登录用户 ID
     * @return 受影响行数 (0 表示非本人或已读)
     */
    int markReadIfOwner(Long id, Long userId);

    /**
     * 标记指定用户+渠道的所有未读通知为已读
     *
     * @param userId  当前登录用户 ID
     * @param channel 渠道 (SYSTEM/SMS/EMAIL)
     * @return 受影响行数
     */
    int markAllRead(Long userId, String channel);

    /**
     * 查询指定用户最近的 N 条通知 (仅 SYSTEM 渠道 + alarm/offline 来源)
     *
     * @param userId 当前登录用户 ID
     * @param limit  返回条数
     * @return 通知列表 (按 create_time DESC)
     */
    List<AlarmNotification> selectUserRecent(Long userId, int limit);

    /**
     * 统计指定用户+渠道的未读通知数
     *
     * @param userId  当前登录用户 ID
     * @param channel 渠道 (SYSTEM/SMS/EMAIL)
     * @return 未读数
     */
    int selectUnreadCount(Long userId, String channel);

    /**
     * 分页查询当前用户未读事件通知（仅 SYSTEM 渠道，alarm/offline 类型）。
     *
     * @param userId   用户 ID
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数
     */
    List<AlarmNotification> selectUserUnreadPage(Long userId, int pageNum, int pageSize);

    /**
     * 当前用户未读事件通知总数（仅 SYSTEM 渠道，alarm/offline 类型）。
     */
    int selectUserUnreadTotal(Long userId);
}
