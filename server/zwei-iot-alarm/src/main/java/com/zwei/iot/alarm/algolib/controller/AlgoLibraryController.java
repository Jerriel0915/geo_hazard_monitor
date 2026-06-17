package com.zwei.iot.alarm.algolib.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.iot.alarm.algolib.domain.AlgoInfo;
import com.zwei.iot.alarm.algolib.domain.dto.AlgoCreateRequest;
import com.zwei.iot.alarm.algolib.domain.dto.AlgoUpdateRequest;
import com.zwei.iot.alarm.algolib.service.IAlgoLibraryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 算法库 Controller。
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/algo-lib")
public class AlgoLibraryController extends BaseController {

    private final IAlgoLibraryService algoLibraryService;

    public AlgoLibraryController(IAlgoLibraryService algoLibraryService) {
        this.algoLibraryService = algoLibraryService;
    }

    @GetMapping("/page")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:list')")
    public TableDataInfo page(AlgoInfo query) {
        startPage();
        List<AlgoInfo> list = algoLibraryService.selectList(query);
        return getDataTable(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:query')")
    public AjaxResult detail(@PathVariable Long id) {
        AlgoInfo info = algoLibraryService.selectDetailById(id);
        if (info == null) return error("算法不存在");
        return success(info);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('iot:algo-library:add')")
    @Log(title = "算法库", businessType = BusinessType.INSERT)
    public AjaxResult create(@Valid @RequestBody AlgoCreateRequest request) {
        AlgoInfo algo = AlgoInfo.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .remark(request.getRemark())
                .createBy(getUsername())
                .build();
        algoLibraryService.insert(algo);
        return AjaxResult.success("新增成功", Map.of("id", algo.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:edit')")
    @Log(title = "算法库", businessType = BusinessType.UPDATE)
    public AjaxResult update(@PathVariable Long id, @Valid @RequestBody AlgoUpdateRequest request) {
        AlgoInfo algo = AlgoInfo.builder()
                .id(id)
                .name(request.getName())
                .description(request.getDescription())
                .remark(request.getRemark())
                .updateBy(getUsername())
                .build();
        return toAjax(algoLibraryService.update(algo));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:edit')")
    @Log(title = "算法库", businessType = BusinessType.UPDATE)
    public AjaxResult updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return error("状态值非法（0-停用 1-启用）");
        }
        return toAjax(algoLibraryService.updateStatus(id, status, getUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('iot:algo-library:remove')")
    @Log(title = "算法库", businessType = BusinessType.DELETE)
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(algoLibraryService.deleteWithVersions(id));
    }
}
