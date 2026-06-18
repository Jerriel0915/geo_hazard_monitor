package com.zwei.datashare.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.datashare.domain.ShareStrategy;
import com.zwei.datashare.domain.ShareStrategyLog;
import com.zwei.datashare.domain.ShareStrategyScript;
import com.zwei.datashare.domain.dto.ShareStrategyCreateRequest;
import com.zwei.datashare.domain.dto.ShareStrategyUpdateRequest;
import com.zwei.datashare.domain.dto.StatusChangeRequest;
import com.zwei.datashare.domain.dto.ShareStrategyVO;
import com.zwei.datashare.enums.StrategyStatus;
import com.zwei.datashare.service.IShareStrategyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 共享策略Controller
 * 提供共享策略的RESTful API接口
 *
 * @author zwei
 */
@RestController
@RequestMapping("/api/v1/datashare/strategy")
public class ShareStrategyController extends BaseController {

    private final IShareStrategyService shareStrategyService;

    @Autowired
    public ShareStrategyController(IShareStrategyService shareStrategyService) {
        this.shareStrategyService = shareStrategyService;
    }

    /**
     * 分页查询共享策略列表
     */
    @PreAuthorize("@ss.hasPermi('basic:device:list')")
    @GetMapping("/page")
    public AjaxResult page(ShareStrategy strategy) {
        startPage();
        List<ShareStrategy> list = shareStrategyService.selectShareStrategyPage(strategy);
        return pageResult(list);
    }


    /**
     * 创建共享策略
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:add')")
    @Log(title = "共享策略管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@Validated @RequestBody ShareStrategyCreateRequest request) {
        ShareStrategy strategy = shareStrategyService.create(request);
        return AjaxResult.success(ShareStrategyVO.fromEntity(strategy));
    }

    /**
     * 更新共享策略
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:edit')")
    @Log(title = "共享策略管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @Validated @RequestBody ShareStrategyUpdateRequest request) {
        ShareStrategy strategy = shareStrategyService.update(id, request);
        return AjaxResult.success(ShareStrategyVO.fromEntity(strategy));
    }

    /**
     * 删除共享策略
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:remove')")
    @Log(title = "共享策略管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        shareStrategyService.delete(id);
        return AjaxResult.success();
    }

    /**
     * 查询共享策略详情
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:query')")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Long id) {
        ShareStrategy strategy = shareStrategyService.findById(id);
        if (strategy == null) {
            return AjaxResult.error("共享策略不存在");
        }
        return AjaxResult.success(ShareStrategyVO.fromEntity(strategy));
    }

    /**
     * 查询共享策略列表
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:list')")
    @GetMapping
    public AjaxResult list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) StrategyStatus status,
            @RequestParam(required = false) String method) {
        List<ShareStrategy> strategies = shareStrategyService.findList(name, status, method);
        List<ShareStrategyVO> vos = strategies.stream()
                .map(ShareStrategyVO::fromEntity)
                .collect(Collectors.toList());
        return AjaxResult.success(vos);
    }

    /**
     * 切换策略状态
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:edit')")
    @Log(title = "共享策略管理", businessType = BusinessType.UPDATE)
    @PatchMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @RequestBody StatusChangeRequest request) {
        ShareStrategy strategy = shareStrategyService.changeStatus(id, request.getStatus());
        return AjaxResult.success(ShareStrategyVO.fromEntity(strategy));
    }

    /**
     * 执行策略
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:execute')")
    @Log(title = "共享策略管理", businessType = BusinessType.OTHER)
    @PostMapping("/{id}/execute")
    public AjaxResult execute(@PathVariable Long id) {
        shareStrategyService.execute(id);
        return AjaxResult.success();
    }

    /**
     * 获取运行日志
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:query')")
    @GetMapping("/{id}/logs")
    public AjaxResult getLogs(@PathVariable Long id) {
        List<ShareStrategyLog> logs = shareStrategyService.findLogs(id);
        return AjaxResult.success(logs);
    }

    /**
     * 获取脚本
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:query')")
    @GetMapping("/{id}/script")
    public AjaxResult getScript(@PathVariable Long id) {
        ShareStrategyScript script = shareStrategyService.getScript(id);
        if (script == null) {
            return AjaxResult.error("脚本不存在");
        }
        return AjaxResult.success(script);
    }

    /**
     * 保存脚本
     */
    @PreAuthorize("@ss.hasPermi('datashare:strategy:edit')")
    @Log(title = "共享策略管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/script")
    public AjaxResult saveScript(@PathVariable Long id,
                                 @RequestParam String script,
                                 @RequestParam(required = false) String variables) {
        shareStrategyService.saveScript(id, script, variables);
        return AjaxResult.success();
    }
}
