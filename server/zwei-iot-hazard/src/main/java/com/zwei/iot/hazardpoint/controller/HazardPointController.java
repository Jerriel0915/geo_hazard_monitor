package com.zwei.iot.hazardpoint.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.StringUtils;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.dto.*;
import com.zwei.iot.video.domain.BoundVideoDeviceVO;
import com.zwei.iot.hazardpoint.service.IDeviceHazardPointService;
import com.zwei.iot.hazardpoint.service.IHazardPointService;
import com.zwei.iot.hazardpoint.service.IVideoDeviceHazardPointService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final IDeviceHazardPointService deviceHazardPointService;
    private final IVideoDeviceHazardPointService videoDeviceHazardPointService;

    @Autowired
    public HazardPointController(IHazardPointService hazardPointService,
                                IDeviceHazardPointService deviceHazardPointService,
                                IVideoDeviceHazardPointService videoDeviceHazardPointService) {
        this.hazardPointService = hazardPointService;
        this.deviceHazardPointService = deviceHazardPointService;
        this.videoDeviceHazardPointService = videoDeviceHazardPointService;
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
        return pageResult(list);
    }

    /**
     * 导出隐患点列表
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:list')")
    @Log(title = "隐患点管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestBody(required = false) HazardPointExportRequest request)
    {
        HazardPoint hazardPoint = buildHazardPointFilter(request);
        List<HazardPoint> list = hazardPointService.selectHazardPointList(hazardPoint);
        List<HazardPointExportVO> exportList = new ArrayList<>(list.size());
        for (HazardPoint item : list)
        {
            HazardPointExportVO vo = new HazardPointExportVO();
            vo.setCode(item.getCode());
            vo.setName(item.getName());
            vo.setGroupName(item.getGroupName());
            vo.setLongitude(item.getLongitude());
            vo.setLatitude(item.getLatitude());
            vo.setStrike(item.getStrike());
            vo.setDescription(item.getDescription());
            vo.setStatusName(item.getStatusName());
            vo.setDeviceCount(item.getDeviceCount());
            vo.setCreateBy(item.getCreateBy());
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateBy(item.getUpdateBy());
            vo.setUpdateTime(item.getUpdateTime());
            exportList.add(vo);
        }
        ExcelUtil<HazardPointExportVO> util = new ExcelUtil<>(HazardPointExportVO.class);
        util.exportExcel(response, exportList, "隐患点数据");
    }

    /**
     * 获取隐患点详情
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:query')")
    @GetMapping("/{id:\\d+}")
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
        return AjaxResult.success("成功", hazardPoint);
    }

    /**
     * 新增隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:add')")
    @Log(title = "隐患点管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HazardPointCreateRequest request)
    {
        if (!hazardPointService.checkHazardPointCodeUnique(request.getCode()))
        {
            return error("新增隐患点失败，编号已存在");
        }
        HazardPoint hazardPoint = buildHazardPoint(request);
        hazardPoint.setCreateBy(getUsername());
        int rows = hazardPointService.insertHazardPoint(hazardPoint);
        return rows > 0
                ? AjaxResult.success("新增成功", Map.of("id", hazardPoint.getId()))
                : AjaxResult.error("新增失败");
    }

    /**
     * 修改隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id:\\d+}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody HazardPointUpdateRequest request)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        HazardPoint hazardPoint = buildHazardPoint(request);
        hazardPoint.setId(id);
        hazardPoint.setUpdateBy(getUsername());
        int rows = hazardPointService.updateHazardPoint(hazardPoint);
        return rows > 0 ? AjaxResult.success("修改成功") : AjaxResult.error("隐患点不存在");
    }

    /**
     * 删除隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:remove')")
    @Log(title = "隐患点管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id:\\d+}")
    public AjaxResult remove(@PathVariable Long id)
    {
        int rows = hazardPointService.deleteHazardPointById(id);
        return rows > 0 ? AjaxResult.success("删除成功") : AjaxResult.error("隐患点不存在或已删除");
    }

    /**
     * 批量删除隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:remove')")
    @Log(title = "隐患点管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch")
    public AjaxResult removeBatch(@Validated @RequestBody BatchIdsRequest request)
    {
        Long[] ids = request.getIds().toArray(Long[]::new);
        int rows = hazardPointService.deleteHazardPointByIds(ids);
        return rows > 0 ? AjaxResult.success("批量删除成功") : AjaxResult.error("批量删除失败");
    }

    /**
     * 停测/恢复隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id:\\d+}/pause")
    public AjaxResult pause(@PathVariable Long id, @Validated @RequestBody HazardPointPauseRequest request)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        int rows = hazardPointService.updateHazardPointPause(id, request.getPause());
        return rows > 0 ? AjaxResult.success("操作成功") : AjaxResult.error("隐患点不存在或已删除");
    }

    /**
     * 完结隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id:\\d+}/complete")
    public AjaxResult complete(@PathVariable Long id)
    {
        if (StringUtils.isNull(id))
        {
            return error("参数错误");
        }
        int rows = hazardPointService.completeHazardPoint(id);
        return rows > 0 ? AjaxResult.success("完结成功") : AjaxResult.error("隐患点不存在或已删除");
    }

    /**
     * 批量操作隐患点(停测/恢复/完结)
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点管理", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/operate")
    public AjaxResult batchOperate(@Validated @RequestBody HazardPointBatchOperateRequest request)
    {
        Long[] ids = request.getIds().toArray(Long[]::new);
        int rows = hazardPointService.batchOperateHazardPoint(ids, request.getOperation());
        return rows > 0 ? AjaxResult.success("操作成功") : AjaxResult.error("批量操作失败");
    }

    // ==================== 4.1 设备隐患点绑定接口 ====================

    /**
     * 获取隐患点已绑定的设备列表
     *
     * @param hpId 隐患点ID
     * @return 已绑定设备列表
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:list')")
    @GetMapping("/{hpId:\\d+}/bound-devices")
    public AjaxResult getBoundDevices(@PathVariable Long hpId)
    {
        List<BoundDeviceVO> list = deviceHazardPointService.getBoundDevices(hpId);
        return AjaxResult.success("成功", list);
    }

    /**
     * 获取未绑定设备列表
     *
     * @param hpId    隐患点ID
     * @param keyword 关键词（设备/传感器名称模糊查询）
     * @return 未绑定设备列表
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:list')")
    @GetMapping("/{hpId:\\d+}/unbound-devices")
    public AjaxResult getUnboundDevices(@PathVariable Long hpId,
                                       @RequestParam(required = false) String keyword)
    {
        List<UnboundDeviceVO> list = deviceHazardPointService.getUnboundDevices(hpId, keyword);
        return AjaxResult.success("成功", list);
    }

    /**
     * 绑定设备到隐患点
     *
     * @param hpId    隐患点ID
     * @param request 绑定请求参数
     * @return 影响行数
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点设备绑定", businessType = BusinessType.INSERT)
    @PostMapping("/{hpId:\\d+}/bind-devices")
    public AjaxResult bindDevices(@PathVariable Long hpId,
                                 @Validated @RequestBody BindDeviceRequest request)
    {
        deviceHazardPointService.bindDevices(hpId, request, getUsername());
        return AjaxResult.success("绑定成功", null);
    }

    /**
     * 解绑设备
     *
     * @param hpId 隐患点ID
     * @param request 解绑请求参数
     * @return 影响行数
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点设备解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/{hpId:\\d+}/unbind-devices")
    public AjaxResult unbindDevices(@PathVariable Long hpId,
                                   @Validated @RequestBody DeviceIdsRequest request)
    {
        deviceHazardPointService.unbindDevices(hpId, request.getDeviceIds());
        return AjaxResult.success("解绑成功", null);
    }

    // ==================== 4.2 视频设备隐患点绑定接口 ====================

    /**
     * 获取隐患点已绑定的视频设备列表
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:list')")
    @GetMapping("/{hpId:\\d+}/bound-video-devices")
    public AjaxResult getBoundVideoDevices(@PathVariable Long hpId)
    {
        List<BoundVideoDeviceVO> list = videoDeviceHazardPointService.getBoundVideoDevices(hpId);
        return AjaxResult.success("成功", list);
    }

    /**
     * 绑定视频设备到隐患点
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点视频设备绑定", businessType = BusinessType.INSERT)
    @PostMapping("/{hpId:\\d+}/bind-video-devices")
    public AjaxResult bindVideoDevices(@PathVariable Long hpId,
                                       @Validated @RequestBody BindVideoDeviceRequest request)
    {
        videoDeviceHazardPointService.bindVideoDevices(hpId, request, getUsername());
        return AjaxResult.success("绑定成功", null);
    }

    /**
     * 从隐患点解绑视频设备
     */
    @PreAuthorize("@ss.hasPermi('iot:hazard-point:edit')")
    @Log(title = "隐患点视频设备解绑", businessType = BusinessType.DELETE)
    @DeleteMapping("/{hpId:\\d+}/unbind-video-devices")
    public AjaxResult unbindVideoDevices(@PathVariable Long hpId,
                                         @Validated @RequestBody VideoDeviceIdsRequest request)
    {
        videoDeviceHazardPointService.unbindVideoDevices(hpId, request.getVideoDeviceIds());
        return AjaxResult.success("解绑成功", null);
    }

    private HazardPoint buildHazardPoint(HazardPointCreateRequest request)
    {
        HazardPoint hazardPoint = new HazardPoint();
        hazardPoint.setCode(trimToNull(request.getCode()));
        hazardPoint.setName(trimToNull(request.getName()));
        hazardPoint.setGroupId(request.getGroupId());
        hazardPoint.setLongitude(request.getLongitude());
        hazardPoint.setLatitude(request.getLatitude());
        hazardPoint.setStrike(request.getStrike());
        hazardPoint.setDescription(trimToNull(request.getDescription()));
        return hazardPoint;
    }

    private HazardPoint buildHazardPoint(HazardPointUpdateRequest request)
    {
        HazardPoint hazardPoint = new HazardPoint();
        hazardPoint.setName(trimToNull(request.getName()));
        hazardPoint.setGroupId(request.getGroupId());
        hazardPoint.setLongitude(request.getLongitude());
        hazardPoint.setLatitude(request.getLatitude());
        hazardPoint.setStrike(request.getStrike());
        hazardPoint.setDescription(trimToNull(request.getDescription()));
        return hazardPoint;
    }

    private String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private HazardPoint buildHazardPointFilter(HazardPointExportRequest request)
    {
        HazardPoint hazardPoint = new HazardPoint();
        if (request == null)
        {
            return hazardPoint;
        }
        hazardPoint.setCode(trimToNull(request.getCode()));
        hazardPoint.setName(trimToNull(request.getName()));
        hazardPoint.setGroupId(request.getGroupId());
        hazardPoint.setStatus(request.getStatus());
        if (request.getIds() != null && !request.getIds().isEmpty())
        {
            hazardPoint.getParams().put("ids", request.getIds());
        }
        return hazardPoint;
    }
}
