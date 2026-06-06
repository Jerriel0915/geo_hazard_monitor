package com.zwei.iot.monitor.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.domain.dto.MonitorTypeCreateRequest;
import com.zwei.iot.monitor.domain.dto.MonitorTypeUpdateRequest;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * 监测类型管理Controller
 * <p>
 * 提供监测类型的RESTful API接口，包括：
 * - 分页查询监测类型列表（GET /api/v1/monitor-types/page）
 * - 获取所有监测类型列表（GET /api/v1/monitor-types）
 * - 获取监测类型详情（GET /api/v1/monitor-types/{id}）
 * - 新增监测类型（POST /api/v1/monitor-types）
 * - 修改监测类型（PUT /api/v1/monitor-types/{id}）
 * - 删除监测类型（DELETE /api/v1/monitor-types/{id}）
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/monitor-types")
public class MonitorTypeController extends BaseController {
    private final IMonitorTypeService monitorTypeService;

    @Autowired
    public MonitorTypeController(IMonitorTypeService monitorTypeService) {
        this.monitorTypeService = monitorTypeService;
    }

    /**
     * 分页查询监测类型列表
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:list')")
    @GetMapping("/page")
    public AjaxResult page(MonitorType monitorType) {
        startPage();
        List<MonitorType> list = monitorTypeService.selectMonitorTypePage(monitorType, 0, 0);
        PageDomain pageDomain = TableSupport.buildPageRequest();
        long total = new PageInfo(list).getTotal();
        HashMap<String, Object> data = new HashMap<>();
        data.put("rows", list);
        data.put("total", total);
        data.put("pageNum", pageDomain.getPageNum());
        data.put("pageSize", pageDomain.getPageSize());
        return AjaxResult.success("成功", data);
    }

    /**
     * 获取所有监测类型列表（不分页）
     * <p>
     * 该接口按文档返回全部监测类型，不接收查询条件。
     *
     * @return 监测类型列表
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:list')")
    @GetMapping
    public AjaxResult list() {
        List<MonitorType> list = monitorTypeService.selectMonitorTypeAll();
        return AjaxResult.success("成功", list);
    }

    /**
     * 获取所有监测类型及其内容（批量加载，避免 N+1）。
     * <p>
     * 单次请求加载全部监测类型及其关联的监测内容，
     * 替代前端先拉列表再逐条请求详情的 N+1 调用模式。
     *
     * @return 含 contents 的监测类型列表
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:list')")
    @GetMapping("/with-contents")
    public AjaxResult listWithContents() {
        List<MonitorType> list = monitorTypeService.selectMonitorTypeAllWithContents();
        return AjaxResult.success("成功", list);
    }

    /**
     * 获取监测类型详情
     *
     * @param id 监测类型ID
     * @return 监测类型详情
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        MonitorType monitorType = monitorTypeService.selectMonitorTypeById(id);
        if (monitorType == null) {
            return error("监测类型不存在");
        }
        return AjaxResult.success("成功", monitorType);
    }

    /**
     * 新增监测类型
     *
     * @param request 新增请求参数
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:add')")
    @Log(title = "监测类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MonitorTypeCreateRequest request) {
        MonitorType monitorType = buildMonitorTypeForCreate(request);
        // 校验编码唯一性
        if (!monitorTypeService.checkMonitorTypeCodeUnique(monitorType)) {
            return error("新增监测类型'" + monitorType.getName() + "'失败，监测类型编码已存在");
        }
        // 设置创建者
        monitorType.setCreateBy(getUsername());
        // 执行新增
        int rows = monitorTypeService.insertMonitorType(monitorType);
        return rows > 0
                ? AjaxResult.success("新增成功", Collections.singletonMap("id", monitorType.getId()))
                : error("新增失败");
    }

    /**
     * 修改监测类型
     *
     * @param id      监测类型ID
     * @param request 修改请求参数
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:edit')")
    @Log(title = "监测类型", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody MonitorTypeUpdateRequest request) {
        if (!request.hasUpdatableField()) {
            return error("修改失败，请至少提供一个可更新字段");
        }
        MonitorType current = monitorTypeService.selectMonitorTypeById(id);
        if (current == null) {
            return error("监测类型不存在");
        }
        MonitorType monitorType = buildMonitorTypeForUpdate(id, request);
        // 设置更新者
        monitorType.setUpdateBy(getUsername());
        // 执行修改
        int rows = monitorTypeService.updateMonitorType(monitorType);
        return rows > 0 ? AjaxResult.success("修改成功") : error("修改失败");
    }

    /**
     * 删除监测类型（逻辑删除）
     *
     * @param id 监测类型ID
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:remove')")
    @Log(title = "监测类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = monitorTypeService.deleteMonitorTypeById(id);
        return rows > 0 ? AjaxResult.success("删除成功") : error("删除失败");
    }

    private MonitorType buildMonitorTypeForCreate(MonitorTypeCreateRequest request) {
        MonitorType monitorType = new MonitorType();
        monitorType.setCode(request.getCode());
        monitorType.setCategoryId(request.getCategoryId());
        monitorType.setName(request.getName());
        monitorType.setIcon(request.getIcon());
        monitorType.setDescription(request.getDescription());
        monitorType.setSortOrder(request.getSortOrder());
        monitorType.setStatus(request.getStatus());
        return monitorType;
    }

    private MonitorType buildMonitorTypeForUpdate(Long id, MonitorTypeUpdateRequest request) {
        MonitorType monitorType = new MonitorType();
        monitorType.setId(id);
        monitorType.setCategoryId(request.getCategoryId());
        monitorType.setName(request.getName());
        monitorType.setIcon(request.getIcon());
        monitorType.setDescription(request.getDescription());
        monitorType.setSortOrder(request.getSortOrder());
        return monitorType;
    }
}
