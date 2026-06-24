package com.zwei.system.notice.service.impl;

import com.zwei.system.notice.domain.SysNotice;
import com.zwei.system.notice.domain.SysNoticeRead;
import com.zwei.system.notice.mapper.SysNoticeReadMapper;
import com.zwei.system.notice.service.ISysNoticeReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 公告已读记录 服务层实现
 *
 * @author zwei
 */
@Service
public class SysNoticeReadServiceImpl implements ISysNoticeReadService
{
    @Autowired
    private SysNoticeReadMapper noticeReadMapper;

    /**
     * 标记已读
     */
    @Override
    public void markRead(Long noticeId, Long userId)
    {
        SysNoticeRead record = new SysNoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        noticeReadMapper.insertNoticeRead(record);
    }

    /**
     * 查询某用户未读公告数量
     */
    @Override
    public int selectUnreadCount(Long userId)
    {
        return noticeReadMapper.selectUnreadCount(userId);
    }

    /**
     * 查询公告列表并标记当前用户已读状态
     */
    @Override
    public List<SysNotice> selectNoticeListWithReadStatus(Long userId, int limit)
    {
        return noticeReadMapper.selectNoticeListWithReadStatus(userId, limit);
    }

    /**
     * 批量标记已读
     */
    @Override
    public void markReadBatch(Long userId, Long[] noticeIds)
    {
        if (noticeIds == null || noticeIds.length == 0)
        {
            return;
        }
        noticeReadMapper.insertNoticeReadBatch(userId, noticeIds);
    }

    /**
     * 查询已阅读某公告的用户列表
     */
    @Override
    public List<Map<String, Object>> selectReadUsersByNoticeId(Long noticeId, String searchValue)
    {
        return noticeReadMapper.selectReadUsersByNoticeId(noticeId, searchValue);
    }

    /**
     * 删除公告时清理对应已读记录
     */
    @Override
    public void deleteByNoticeIds(Long[] noticeIds)
    {
        noticeReadMapper.deleteByNoticeIds(noticeIds);
    }

    @Override
    public List<SysNotice> selectNoticePage(Long userId, int pageNum, int pageSize)
    {
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(pageSize, 50));
        int offset = (safePage - 1) * safeSize;
        return noticeReadMapper.selectNoticePageWithReadStatus(userId, offset, safeSize);
    }

    @Override
    public int selectNoticeCount()
    {
        return noticeReadMapper.selectNoticeCountWithReadStatus(null);
    }
}
