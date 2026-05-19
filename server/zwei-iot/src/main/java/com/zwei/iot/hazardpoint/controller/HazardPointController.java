package com.zwei.iot.hazardpoint.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.StringUtils;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

/**
 * 隐患点管理
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/hazard-points")
public class HazardPointController extends BaseController
{
    private final IHazardPointService hazardPointService;

    @Autowired
    public HazardPointController(IHazardPointService hazardPointService) {
        this.hazardPointService = hazardPointService;
    }

    /**
     * 分页查询隐患点列表
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:list')")
    @GetMapping("/page")
    public AjaxResult list(HazardPoint hazardPoint)
    {
        startPage();
        List<HazardPoint> list = hazardPointService.selectHazardPointList(hazardPoint);
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
     * 获取隐患点详情
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        HazardPoint hazardPoint = hazardPointService.selectHazardPointById(id);
        if (hazardPoint == null)
        {
            return error("隐患点不存在");
        }
        return success(hazardPoint);
    }

    /**
     * 新增隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:add')")
    @Log(title = "隐患点管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HazardPoint hazardPoint)
    {
        if (!hazardPointService.checkHazardPointCodeUnique(hazardPoint.getCode()))
        {
            return error("新增隐患点失败，编号已存在");
        }
        hazardPoint.setCreateBy(getUsername());
        return toAjax(hazardPointService.insertHazardPoint(hazardPoint));
    }

    /**
     * 修改隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody HazardPoint hazardPoint)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        hazardPoint.setId(id);
        hazardPoint.setUpdateBy(getUsername());
        return toAjax(hazardPointService.updateHazardPoint(hazardPoint));
    }

    /**
     * 删除隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:remove')")
    @Log(title = "隐患点管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(hazardPointService.deleteHazardPointById(id));
    }

    /**
     * 批量删除隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:remove')")
    @Log(title = "隐患点管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult removeBatch(@RequestBody Long[] ids)
    {
        return toAjax(hazardPointService.deleteHazardPointByIds(ids));
    }

    /**
     * 停测/恢复隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/pause")
    public AjaxResult pause(@PathVariable Long id, @RequestBody(required = false) Boolean pause)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        // pause参数为空时默认为true(停测)
        boolean isPause = pause == null || pause;
        return toAjax(hazardPointService.updateHazardPointPause(id, isPause));
    }

    /**
     * 完结隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/complete")
    public AjaxResult complete(@PathVariable Long id)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        return toAjax(hazardPointService.completeHazardPoint(id));
    }

    /**
     * 批量操作隐患点(停测/恢复/完结)
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/operate")
    public AjaxResult batchOperate(@RequestBody HashMap<String, Object> params)
    {
        @SuppressWarnings("unchecked")
        List<Integer> idList = (List<Integer>) params.get("ids");
        String operation = (String) params.get("operation");

        if (idList == null || idList.isEmpty())
        {
            return error("请选择要操作的隐患点");
        }
        if (StringUtils.isEmpty(operation))
        {
            return error("操作类型不能为空");
        }

        // JSON反序列化时ids为Integer数组，需转换为Long[]
        Long[] ids = idList.stream().map(Integer::longValue).toArray(Long[]::new);
        return toAjax(hazardPointService.batchOperateHazardPoint(ids, operation));
    }
}
