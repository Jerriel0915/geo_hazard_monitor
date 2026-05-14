package com.zwei.iot.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.domain.MonitorType;
import com.zwei.iot.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
     *
     * @param monitorType 查询条件
     * @return 监测类型列表
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:list')")
    @GetMapping
    public AjaxResult list(MonitorType monitorType) {
        List<MonitorType> list = monitorTypeService.selectMonitorTypeAll();
        return success(list);
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
        return success(monitorType);
    }

    /**
     * 新增监测类型
     *
     * @param monitorType 监测类型信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:add')")
    @Log(title = "监测类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MonitorType monitorType) {
        // 校验编码唯一性
        if (!monitorTypeService.checkMonitorTypeCodeUnique(monitorType)) {
            return error("新增监测类型'" + monitorType.getName() + "'失败，监测类型编码已存在");
        }
        // 设置创建者
        monitorType.setCreateBy(getUsername());
        // 执行新增
        int rows = monitorTypeService.insertMonitorType(monitorType);
        return rows > 0 ? success(monitorType.getId()) : error("新增失败");
    }

    /**
     * 修改监测类型
     *
     * @param id          监测类型ID
     * @param monitorType 监测类型信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:monitorType:edit')")
    @Log(title = "监测类型", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody MonitorType monitorType) {
        // 设置ID
        monitorType.setId(id);
        // 校验编码唯一性
        if (!monitorTypeService.checkMonitorTypeCodeUnique(monitorType)) {
            return error("修改监测类型'" + monitorType.getName() + "'失败，监测类型编码已存在");
        }
        // 设置更新者
        monitorType.setUpdateBy(getUsername());
        // 执行修改
        int rows = monitorTypeService.updateMonitorType(monitorType);
        return rows > 0 ? success() : error("修改失败");
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
        return rows > 0 ? success() : error("删除失败");
    }
}
