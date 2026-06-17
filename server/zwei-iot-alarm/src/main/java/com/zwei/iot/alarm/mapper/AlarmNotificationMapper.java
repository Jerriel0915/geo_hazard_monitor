package com.zwei.iot.alarm.mapper;

import com.zwei.iot.alarm.domain.AlarmNotification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmNotificationMapper {

    List<AlarmNotification> selectNotificationList(AlarmNotification notification);

    List<AlarmNotification> selectByAlarmId(Long alarmId);

    int insertNotification(AlarmNotification notification);

    int batchInsert(List<AlarmNotification> notifications);

    int updateStatus(@org.apache.ibatis.annotations.Param("id") Long id,
                     @org.apache.ibatis.annotations.Param("status") Integer status,
                     @org.apache.ibatis.annotations.Param("sendTime") String sendTime,
                     @org.apache.ibatis.annotations.Param("errorMsg") String errorMsg);

    /**
     * 仅当 userId 为该通知接收人时，标记为已读 (幂等: read_time IS NULL 才更新)
     */
    int markReadIfOwner(@org.apache.ibatis.annotations.Param("id") Long id,
                        @org.apache.ibatis.annotations.Param("userId") Long userId);

    /**
     * 标记指定用户+渠道的所有未读通知为已读
     */
    int markAllRead(@org.apache.ibatis.annotations.Param("userId") Long userId,
                    @org.apache.ibatis.annotations.Param("channel") String channel);

    /**
     * 查询指定用户最近的 N 条通知 (仅 SYSTEM 渠道 + alarm/offline 来源)
     */
    List<AlarmNotification> selectUserRecent(@org.apache.ibatis.annotations.Param("userId") Long userId,
                                             @org.apache.ibatis.annotations.Param("limit") int limit);

    /**
     * 统计指定用户+渠道的未读通知数
     */
    int selectUnreadCount(@org.apache.ibatis.annotations.Param("userId") Long userId,
                          @org.apache.ibatis.annotations.Param("channel") String channel);
}
