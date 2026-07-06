package com.zwei.web.controller.system.notice;

import com.zwei.common.annotation.Log;
import com.zwei.common.annotation.RepeatSubmit;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.core.text.Convert;
import com.zwei.common.enums.BusinessType;
import com.zwei.system.notice.domain.SysNotice;
import com.zwei.system.notice.service.ISysNoticeReadService;
import com.zwei.system.notice.service.ISysNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告 信息操作处理
 * 
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    @Autowired
    private ISysNoticeReadService noticeReadService;

    /**
     * 获取通知公告列表
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     */
    @RepeatSubmit
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        if (!noticeService.checkNoticeTitleUnique(notice.getNoticeTitle(), null)) {
            return error("新增失败，公告标题已存在");
        }
        notice.setNoticeId(null); // 防止 mass-assignment: 客户端不应指定 ID
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        if (!noticeService.checkNoticeTitleUnique(notice.getNoticeTitle(), notice.getNoticeId())) {
            return error("修改失败，公告标题已存在");
        }
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 首页顶部公告（通知中心面板用），支持分页。
     * 返回结构：{ code, msg, data: SysNotice[], total, unreadCount, timestamp }
     *
     * @param status 公告状态: '0'=当前公告(默认) '1'=历史公告
     */
    @GetMapping("/listTop")
    @ResponseBody
    public AjaxResult listTop(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(defaultValue = "0") String status)
    {
        Long userId = getUserId();
        int safePage = Math.max(1, pageNum);
        int safeSize = Math.max(1, Math.min(pageSize, 50));

        List<SysNotice> list = noticeReadService.selectNoticePage(userId, safePage, safeSize, status);
        int total = noticeReadService.selectNoticeCount(status);
        int unreadCount = noticeReadService.selectUnreadCount(userId);

        AjaxResult ajax = AjaxResult.success(list);
        ajax.put("total", total);
        ajax.put("unreadCount", unreadCount);
        ajax.put("timestamp", System.currentTimeMillis());
        return ajax;
    }

    /**
     * 标记公告已读
     */
    @PostMapping("/markRead")
    @ResponseBody
    public AjaxResult markRead(Long noticeId)
    {
        Long userId = getUserId();
        noticeReadService.markRead(noticeId, userId);
        return success();
    }

    /**
     * 批量标记已读
     */
    @PostMapping("/markReadAll")
    @ResponseBody
    public AjaxResult markReadAll(String ids)
    {
        Long userId = getUserId();
        Long[] noticeIds = Convert.toLongArray(ids);
        noticeReadService.markReadBatch(userId, noticeIds);
        return success();
    }

    /**
     * 标记当前用户所有未读公告为已读（无需参数，服务端 INSERT...SELECT 完成）。
     */
    @PostMapping("/markAllRead")
    @ResponseBody
    public AjaxResult markAllRead()
    {
        Long userId = getUserId();
        noticeReadService.markAllReadForUser(userId);
        return success();
    }

    /**
     * 删除通知公告
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable String noticeIds)
    {
        Long[] ids = Convert.toLongArray(noticeIds);
        noticeReadService.deleteByNoticeIds(ids);
        return toAjax(noticeService.deleteNoticeByIds(ids));
    }

    /**
     * 已读用户列表数据
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/readUsers/list")
    @ResponseBody
    public TableDataInfo readUsersList(Long noticeId, String searchValue)
    {
        startPage();
        List<?> list = noticeReadService.selectReadUsersByNoticeId(noticeId, searchValue);
        return getDataTable(list);
    }

}
