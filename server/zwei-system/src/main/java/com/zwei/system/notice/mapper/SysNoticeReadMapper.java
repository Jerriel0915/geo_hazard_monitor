package com.zwei.system.notice.mapper;

import com.zwei.system.notice.domain.SysNotice;
import com.zwei.system.notice.domain.SysNoticeRead;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 公告已读记录 数据层
 *
 * @author zwei
 */
public interface SysNoticeReadMapper
{
    /**
     * 新增已读记录（忽略重复）
     *
     * @param noticeRead 已读记录
     * @return 结果
     */
    public int insertNoticeRead(SysNoticeRead noticeRead);

    /**
     * 查询某用户未读公告数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public int selectUnreadCount(@Param("userId") Long userId);

    /**
     * 查询某用户是否已读某公告
     *
     * @param noticeId 公告ID
     * @param userId   用户ID
     * @return 已读记录数（0未读 1已读）
     */
    public int selectIsRead(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    /**
     * 批量标记已读
     *
     * @param userId    用户ID
     * @param noticeIds 公告ID数组
     * @return 结果
     */
    public int insertNoticeReadBatch(@Param("userId") Long userId, @Param("noticeIds") Long[] noticeIds);

    /**
     * 查询带已读状态的公告列表（SQL层限制条数，一次查询完成）
     *
     * @param userId 用户ID
     * @param limit  最多返回条数
     * @return 带 isRead 标记的公告列表
     */
    public List<SysNotice> selectNoticeListWithReadStatus(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询已阅读某公告的用户列表
     *
     * @param noticeId 公告ID
     * @param searchValue 搜索值
     * @return 已读用户列表
     */
    public List<Map<String, Object>> selectReadUsersByNoticeId(@Param("noticeId") Long noticeId, @Param("searchValue") String searchValue);

    /**
     * 公告删除时清理对应已读记录
     *
     * @param noticeIds 公告ID数组
     * @return 结果
     */
    public int deleteByNoticeIds(@Param("noticeIds") Long[] noticeIds);

    /**
     * 分页查询当前用户的公告列表（带已读状态）。
     * 按 notice_id DESC 排序。
     *
     * @param status 公告状态: '0'=当前公告 '1'=历史公告
     */
    List<SysNotice> selectNoticePageWithReadStatus(@Param("userId") Long userId,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit,
                                                   @Param("status") String status);

    /**
     * 公告总数（按 status 过滤，用于分页 total）。
     */
    int selectNoticeCountWithReadStatus(@Param("userId") Long userId,
                                        @Param("status") String status);

    /**
     * 标记当前用户所有未读的当前公告(status=0)为已读。
     * INSERT...SELECT 单次 SQL，避免客户端收集 ID 再批量插入。
     *
     * @param userId 当前用户ID
     * @return 插入行数
     */
    int insertAllUnreadForUser(@Param("userId") Long userId);
}
