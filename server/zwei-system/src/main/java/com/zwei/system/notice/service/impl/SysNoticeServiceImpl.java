package com.zwei.system.notice.service.impl;

import com.zwei.common.event.NoticeCreatedEvent;
import com.zwei.common.exception.ServiceException;
import com.zwei.system.notice.domain.SysNotice;
import com.zwei.system.notice.mapper.SysNoticeMapper;
import com.zwei.system.notice.service.ISysNoticeService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 公告 服务层实现
 *
 * @author zwei
 */
@Service
public class SysNoticeServiceImpl implements ISysNoticeService
{
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysNoticeMapper noticeMapper;
    private final ApplicationEventPublisher eventPublisher;

    public SysNoticeServiceImpl(SysNoticeMapper noticeMapper, ApplicationEventPublisher eventPublisher) {
        this.noticeMapper = noticeMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public SysNotice selectNoticeById(Long noticeId) {
        return noticeMapper.selectNoticeById(noticeId);
    }

    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice) {
        return noticeMapper.selectNoticeList(notice);
    }

    @Override
    public int insertNotice(SysNotice notice) {
        if (!checkNoticeTitleUnique(notice.getNoticeTitle(), null)) {
            throw new ServiceException("新增失败，公告标题已存在");
        }
        int rows = noticeMapper.insertNotice(notice);
        if (rows > 0 && "0".equals(notice.getStatus())) {
            eventPublisher.publishEvent(new NoticeCreatedEvent(
                    notice.getNoticeId(),
                    notice.getNoticeTitle(),
                    notice.getNoticeContent(),
                    notice.getNoticeType(),
                    LocalDateTime.now().format(DT_FMT)
            ));
        }
        return rows;
    }

    /**
     * 修改公告
     *
     * @param notice 公告信息
     * @return 结果
     */
    @Override
    public int updateNotice(SysNotice notice)
    {
        if (!checkNoticeTitleUnique(notice.getNoticeTitle(), notice.getNoticeId())) {
            throw new ServiceException("修改失败，公告标题已存在");
        }
        return noticeMapper.updateNotice(notice);
    }

    /**
     * 删除公告对象
     *
     * @param noticeId 公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeById(Long noticeId)
    {
        return noticeMapper.deleteNoticeById(noticeId);
    }

    /**
     * 批量删除公告信息
     *
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    @Override
    public int deleteNoticeByIds(Long[] noticeIds)
    {
        return noticeMapper.deleteNoticeByIds(noticeIds);
    }

    @Override
    public boolean checkNoticeTitleUnique(String noticeTitle, Long noticeId) {
        if (noticeTitle == null || noticeTitle.isEmpty()) {
            return true;
        }
        return noticeMapper.checkNoticeTitleUnique(noticeTitle, noticeId) == null;
    }
}
