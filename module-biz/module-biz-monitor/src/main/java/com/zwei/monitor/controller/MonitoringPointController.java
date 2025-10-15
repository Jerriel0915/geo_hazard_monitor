package com.zwei.monitor.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.monitor.domain.MonitoringPoint;
import com.zwei.monitor.domain.PointDevice;
import com.zwei.monitor.service.IMonitoringPointService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
 * 监测点位(测站点)Controller
 * 
 * @author zwei
 * @date 2025-10-15
 */
@Api(tags = "监测点位管理")
@Slf4j
@RestController
@RequestMapping("/monitor/point")
public class MonitoringPointController extends BaseController
{
    @Autowired
    private IMonitoringPointService monitoringPointService;

    /**
     * 查询监测点位(测站点)列表
     */
    @ApiOperation("获取监测点位(测站点)列表")
    @PreAuthorize("@ss.hasPermi('monitor:point:list')")
    @GetMapping("/list")
    public TableDataInfo list(MonitoringPoint monitoringPoint)
    {
        startPage();
        List<MonitoringPoint> list = monitoringPointService.selectMonitoringPointList(monitoringPoint);
        return getDataTable(list);
    }

    /**
     * 根据对象ID查询监测点位列表
     */
    @ApiOperation("根据对象ID查询监测点位列表")
    @PreAuthorize("@ss.hasPermi('monitor:point:list')")
    @GetMapping("/byObject/{objectId}")
    public AjaxResult getByObjectId(@PathVariable("objectId") Long objectId)
    {
        List<MonitoringPoint> points = monitoringPointService.selectMonitoringPointByObjectId(objectId);
        return AjaxResult.success(points);
    }

    /**
     * 导出监测点位(测站点)列表
     */
    @ApiOperation("导出监测点位(测站点)列表")
    @PreAuthorize("@ss.hasPermi('monitor:point:export')")
    @Log(title = "监测点位(测站点)", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MonitoringPoint monitoringPoint)
    {
        List<MonitoringPoint> list = monitoringPointService.selectMonitoringPointList(monitoringPoint);
        ExcelUtil<MonitoringPoint> util = new ExcelUtil<MonitoringPoint>(MonitoringPoint.class);
        util.exportExcel(response, list, "监测点位(测站点)数据");
    }

    /**
     * 获取监测点位(测站点)详细信息
     */
    @ApiOperation("获取监测点位(测站点)详细信息")
    @PreAuthorize("@ss.hasPermi('monitor:point:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(monitoringPointService.selectMonitoringPointById(id));
    }

    /**
     * 新增监测点位(测站点)
     */
    @ApiOperation("新增监测点位(测站点)")
    @PreAuthorize("@ss.hasPermi('monitor:point:add')")
    @Log(title = "监测点位(测站点)", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MonitoringPoint monitoringPoint)
    {
        return toAjax(monitoringPointService.insertMonitoringPoint(monitoringPoint));
    }

    /**
     * 修改监测点位(测站点)
     */
    @ApiOperation("修改监测点位(测站点)")
    @PreAuthorize("@ss.hasPermi('monitor:point:edit')")
    @Log(title = "监测点位(测站点)", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MonitoringPoint monitoringPoint)
    {
        return toAjax(monitoringPointService.updateMonitoringPoint(monitoringPoint));
    }

    /**
     * 删除监测点位(测站点)
     */
    @ApiOperation("删除监测点位(测站点)")
    @PreAuthorize("@ss.hasPermi('monitor:point:remove')")
    @Log(title = "监测点位(测站点)", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(monitoringPointService.deleteMonitoringPointByIds(ids));
    }
    
    /**
     * 查询监测点位关联的设备信息
     */
    @ApiOperation("查询监测点位关联的设备信息")
    @PreAuthorize("@ss.hasPermi('monitor:point:list')")
    @GetMapping("/{pointId}/devices")
    public AjaxResult getAssociatedDevices(@PathVariable("pointId") Long pointId)
    {
        log.debug("查询监测点位ID: {} 关联的设备信息", pointId);
        List<PointDevice> devices = monitoringPointService.selectByPointId(pointId);
        return AjaxResult.success(devices);
    }
}