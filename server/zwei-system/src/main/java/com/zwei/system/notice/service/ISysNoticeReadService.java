package com.zwei.system.notice.service;

import com.zwei.system.notice.domain.SysNotice;

import java.util.List;
import java.util.Map;

/**
 * 公告已读记录 服务层
 *
 * @author zwei
 */
public interface ISysNoticeReadService
{
    /**
     * 标记已读（幂等，重复调用不报错）
     *
     * @param noticeId 公告ID
     * @param userId   用户ID
     */
    public void markRead(Long noticeId, Long userId);

    /**
     * 查询某用户未读公告数量
     *
     * @param userId 用户ID
     * @return 未读数量
     */
    public int selectUnreadCount(Long userId);

    /**
     * 查询公告列表并标记当前用户已读状态（用于首页展示）
     *
     * @param userId 用户ID
     * @param limit  最多返回条数
     * @return 带 isRead 标记的公告列表
     */
    public List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit);

    /**
     * 批量标记已读
     *
     * @param userId    用户ID
     * @param noticeIds 公告ID数组
     */
    public void markReadBatch(Long userId, Long[] noticeIds);

    /**
     * 查询已阅读某公告的用户列表
     *
     * @param noticeId  公告ID
     * @param searchValue 搜索值
     * @return 已读用户列表
     */
    public List<Map<String, Object>> selectReadUsersByNoticeId(Long noticeId, String searchValue);

    /**
     * 删除公告时清理对应已读记录
     *
     * @param noticeIds 公告ID数组
     */
    public void deleteByNoticeIds(Long[] noticeIds);

    /**
     * 分页查询当前用户可见的公告列表（带 isRead 标记）。
     *
     * @param userId     当前用户 ID
     * @param pageNum    页码，从 1 开始
     * @param pageSize   每页条数
     * @param status     公告状态: '0'=当前公告 '1'=历史公告
     * @param readFilter 已读筛选: 'unread'=仅未读 'all'=全部
     * @return 公告列表
     */
    List<SysNotice> selectNoticePage(Long userId, int pageNum, int pageSize, String status, String readFilter);

    /**
     * 当前用户可见公告总数（用于分页 total）。
     *
     * @param status     公告状态过滤
     * @param readFilter 已读筛选: 'unread'=仅未读 'all'=全部
     */
    int selectNoticeCount(String status, String readFilter);

    /**
     * 标记当前用户所有未读的当前公告(status=0)为已读。
     * 单次 SQL (INSERT...SELECT)，不依赖客户端 ID 传递。
     *
     * @param userId 当前用户ID
     */
    void markAllReadForUser(Long userId);
}
