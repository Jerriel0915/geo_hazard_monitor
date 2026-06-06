package com.zwei.iot.monitor.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.monitor.domain.MonitorCategory;
import com.zwei.iot.monitor.domain.dto.MonitorCategoryCreateRequest;
import com.zwei.iot.monitor.domain.dto.MonitorCategoryUpdateRequest;
import com.zwei.iot.monitor.service.IMonitorCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("api/v1/monitor-categories")
public class MonitorCategoryController extends BaseController {
    private final IMonitorCategoryService service;

    @Autowired
    public MonitorCategoryController(IMonitorCategoryService service) { this.service = service; }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:list')")
    @GetMapping("/page")
    public AjaxResult page(MonitorCategory m) {
        startPage();
        List<MonitorCategory> list = service.selectMonitorCategoryPage(m, 0, 0);
        PageDomain pd = TableSupport.buildPageRequest();
        long total = new PageInfo(list).getTotal();
        Map<String, Object> data = new HashMap<>();
        data.put("rows", list); data.put("total", total);
        data.put("pageNum", pd.getPageNum()); data.put("pageSize", pd.getPageSize());
        return AjaxResult.success("成功", data);
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:list')")
    @GetMapping
    public AjaxResult list() { return AjaxResult.success("成功", service.selectMonitorCategoryAll()); }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        MonitorCategory mc = service.selectMonitorCategoryById(id);
        return mc == null ? error("监测大类不存在") : AjaxResult.success("成功", mc);
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:add')")
    @Log(title = "监测大类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MonitorCategoryCreateRequest req) {
        MonitorCategory mc = new MonitorCategory();
        mc.setCode(req.getCode()); mc.setName(req.getName());
        mc.setIcon(req.getIcon()); mc.setSortOrder(req.getSortOrder()); mc.setStatus(req.getStatus());
        if (!service.checkMonitorCategoryCodeUnique(mc)) return error("新增监测大类'" + mc.getName() + "'失败，编码已存在");
        mc.setCreateBy(getUsername());
        return service.insertMonitorCategory(mc) > 0 ? AjaxResult.success("新增成功", Collections.singletonMap("id", mc.getId())) : error("新增失败");
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:edit')")
    @Log(title = "监测大类", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody MonitorCategoryUpdateRequest req) {
        if (!req.hasUpdatableField()) return error("修改失败，请至少提供一个可更新字段");
        if (service.selectMonitorCategoryById(id) == null) return error("监测大类不存在");
        MonitorCategory mc = new MonitorCategory();
        mc.setId(id); mc.setName(req.getName());
        mc.setIcon(req.getIcon()); mc.setSortOrder(req.getSortOrder()); mc.setStatus(req.getStatus());
        mc.setUpdateBy(getUsername());
        return service.updateMonitorCategory(mc) > 0 ? AjaxResult.success("修改成功") : error("修改失败");
    }

    @PreAuthorize("@ss.hasPermi('basic:monitorCategory:remove')")
    @Log(title = "监测大类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) { return service.deleteMonitorCategoryById(id) > 0 ? AjaxResult.success("删除成功") : error("删除失败"); }
}
