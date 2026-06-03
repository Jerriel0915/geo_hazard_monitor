package com.zwei.log.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.pagehelper.PageInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.log.api.dto.AuthLogQuery;
import com.zwei.log.api.dto.OperationLogQuery;
import com.zwei.log.api.dto.RuntimeLogQuery;
import com.zwei.log.application.service.LogCenterService;
import com.zwei.log.domain.model.LogAuthRecord;
import com.zwei.log.domain.model.LogOperationRecord;
import com.zwei.log.domain.model.LogRuntimeRecord;

/**
 * 日志查询接口
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogQueryController extends BaseController {

    private final LogCenterService logCenterService;

    public LogQueryController(LogCenterService logCenterService) {
        this.logCenterService = logCenterService;
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/operations/page")
    public AjaxResult operationPage(OperationLogQuery query) {
        startPage();
        List<LogOperationRecord> rows = logCenterService.queryOperation(query);
        return AjaxResult.success("成功", buildPageData(rows));
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/auth/page")
    public AjaxResult authPage(AuthLogQuery query) {
        startPage();
        List<LogAuthRecord> rows = logCenterService.queryAuth(query);
        return AjaxResult.success("成功", buildPageData(rows));
    }

    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/runtime/page")
    public AjaxResult runtimePage(RuntimeLogQuery query) {
        startPage();
        List<LogRuntimeRecord> rows = logCenterService.queryRuntime(query);
        return AjaxResult.success("成功", buildPageData(rows));
    }

    private Map<String, Object> buildPageData(List<?> rows) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Map<String, Object> data = new HashMap<>(4);
        data.put("total", new PageInfo<>(rows).getTotal());
        data.put("rows", rows);
        data.put("pageNum", pageDomain.getPageNum());
        data.put("pageSize", pageDomain.getPageSize());
        return data;
    }
}
