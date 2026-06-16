package com.zwei.iot.video.controller;

import com.github.pagehelper.PageInfo;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.PageDomain;
import com.zwei.common.core.page.TableSupport;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.iot.video.domain.VideoDevice;
import com.zwei.iot.video.domain.dto.VideoDeviceExportVO;
import com.zwei.iot.video.service.IVideoDeviceService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 视频设备管理Controller
 * <p>
 * 提供视频设备的RESTful API接口，包括：
 * - 分页查询视频设备列表（GET /api/v1/video-devices/page）
 * - 获取视频设备详情（GET /api/v1/video-devices/{id}）
 * - 新增视频设备（POST /api/v1/video-devices）
 * - 修改视频设备（PUT /api/v1/video-devices/{id}）
 * - 删除视频设备（DELETE /api/v1/video-devices/{id}）
 *
 * @author zwei
 */
@RestController
@RequestMapping("api/v1/video-devices")
public class VideoDeviceController extends BaseController {
    private final IVideoDeviceService videoDeviceService;

    @Autowired
    public VideoDeviceController(IVideoDeviceService videoDeviceService) {
        this.videoDeviceService = videoDeviceService;
    }

    /**
     * 分页查询视频设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:list')")
    @GetMapping("/page")
    public AjaxResult page(VideoDevice videoDevice) {
        startPage();
        List<VideoDevice> list = videoDeviceService.selectVideoDevicePage(videoDevice, 0, 0);
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
     * 获取所有视频设备列表（不分页）
     *
     * @param videoDevice 查询条件
     * @return 视频设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:list')")
    @GetMapping
    public AjaxResult list(VideoDevice videoDevice) {
        List<VideoDevice> list = videoDeviceService.selectVideoDeviceAll();
        return success(list);
    }

    /**
     * 获取视频设备详情
     *
     * @param id 视频设备ID
     * @return 视频设备
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        VideoDevice videoDevice = videoDeviceService.selectVideoDeviceById(id);
        if (videoDevice == null) {
            return error("视频设备不存在");
        }
        return success(videoDevice);
    }

    /**
     * 新增视频设备
     *
     * @param videoDevice 视频设备信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:add')")
    @Log(title = "视频设备", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VideoDevice videoDevice) {
        // 校验编码唯一性
        if (!videoDeviceService.checkVideoDeviceCodeUnique(videoDevice)) {
            return error("新增视频设备'" + videoDevice.getName() + "'失败，设备编号已存在");
        }
        // 设置创建者
        videoDevice.setCreateBy(getUsername());
        // 执行新增
        int rows = videoDeviceService.insertVideoDevice(videoDevice);
        return rows > 0 ? success(videoDevice.getId()) : error("新增失败");
    }

    /**
     * 修改视频设备
     *
     * @param id          视频设备ID
     * @param videoDevice 视频设备信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:edit')")
    @Log(title = "视频设备", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody VideoDevice videoDevice) {
        // 设置ID
        videoDevice.setId(id);
        // 校验编码唯一性
        if (!videoDeviceService.checkVideoDeviceCodeUnique(videoDevice)) {
            return error("修改视频设备'" + videoDevice.getName() + "'失败，设备编号已存在");
        }
        // 设置更新者
        videoDevice.setUpdateBy(getUsername());
        // 执行修改
        int rows = videoDeviceService.updateVideoDevice(videoDevice);
        return rows > 0 ? success() : error("修改失败");
    }

    /**
     * 删除视频设备（逻辑删除）
     *
     * @param id 视频设备ID
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:remove')")
    @Log(title = "视频设备", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        int rows = videoDeviceService.deleteVideoDeviceById(id);
        return rows > 0 ? success() : error("删除失败");
    }

    /**
     * 导出视频设备列表
     */
    @PreAuthorize("@ss.hasPermi('basic:videoDevice:list')")
    @Log(title = "视频设备", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VideoDevice videoDevice) {
        List<VideoDevice> list = videoDeviceService.selectVideoDeviceAll();
        List<VideoDeviceExportVO> exportList = new ArrayList<>(list.size());
        for (VideoDevice item : list) {
            VideoDeviceExportVO vo = new VideoDeviceExportVO();
            vo.setCode(item.getCode());
            vo.setName(item.getName());
            vo.setProtocolCode(item.getProtocolCode());
            vo.setProtocolName(item.getProtocolName());
            vo.setStreamUrl(item.getStreamUrl());
            vo.setLongitude(item.getLongitude());
            vo.setLatitude(item.getLatitude());
            vo.setStatusName(item.getStatus() == null ? null
                    : item.getStatus() == 0 ? "离线" : item.getStatus() == 1 ? "在线" : "故障");
            vo.setLastOnlineTime(item.getLastOnlineTime());
            vo.setInstallTime(item.getInstallTime());
            vo.setCreateBy(item.getCreateBy());
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateBy(item.getUpdateBy());
            vo.setUpdateTime(item.getUpdateTime());
            exportList.add(vo);
        }
        ExcelUtil<VideoDeviceExportVO> util = new ExcelUtil<>(VideoDeviceExportVO.class);
        util.exportExcel(response, exportList, "视频设备数据");
    }
}