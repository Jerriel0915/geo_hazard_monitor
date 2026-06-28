package com.zwei.iot.alarm.algolib.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.config.RuoYiConfig;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.domain.AlgoResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.exception.ServiceException;
import com.zwei.iot.alarm.algolib.domain.AlgoVersion;
import com.zwei.iot.alarm.algolib.service.IAlgoVersionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * 算法版本 Controller。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/algo-lib")
public class AlgoVersionController extends BaseController {

    private final IAlgoVersionService versionService;

    public AlgoVersionController(IAlgoVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping("/{algoId}/versions")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult listVersions(@PathVariable Long algoId) {
        List<AlgoVersion> list = versionService.selectByAlgoId(algoId);
        return success(list);
    }

    @PostMapping("/{algoId}/versions/upload")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:upload')")
    @Log(title = "算法库版本", businessType = BusinessType.INSERT)
    public AjaxResult upload(@PathVariable Long algoId,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam("versionNo") String versionNo,
                             @RequestParam(value = "remark", required = false) String remark) {
        Long versionId = versionService.upload(algoId, versionNo, remark, file, getUsername());
        return AjaxResult.success("上传成功", Map.of("id", versionId));
    }

    @DeleteMapping("/versions/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:remove')")
    @Log(title = "算法库版本", businessType = BusinessType.DELETE)
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(versionService.delete(id));
    }

    @GetMapping("/versions/{id}/download")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        AlgoVersion version = versionService.selectById(id);
        if (version == null) {
            throw new ServiceException("版本不存在或已删除");
        }
        File file = new File(RuoYiConfig.getProfile() + File.separator + version.getFileName());
        if (!file.exists()) {
            throw new ServiceException("算法包文件不存在: " + version.getFileName());
        }
        try {
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" +
                            URLEncoder.encode(version.getOriginalName(), StandardCharsets.UTF_8) + "\"");
            Files.copy(file.toPath(), response.getOutputStream());
        } catch (IOException e) {
            throw new ServiceException("下载失败: " + e.getMessage());
        }
    }

    @GetMapping("/{algoCode}/versions/{versionNo}/describe")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult describe(@PathVariable String algoCode,
                               @PathVariable String versionNo) {
        com.zwei.common.domain.AlgoResult result = versionService.describe(algoCode, versionNo);
        if (result.success()) {
            return success(result.data());
        }
        return error(result.error());
    }

    @GetMapping("/{algoCode}/describe-latest")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult describeLatest(@PathVariable String algoCode) {
        AlgoResult result = versionService.describeLatest(algoCode);
        if (result.success()) {
            return success(result.data());
        }
        return error(result.error());
    }
}
