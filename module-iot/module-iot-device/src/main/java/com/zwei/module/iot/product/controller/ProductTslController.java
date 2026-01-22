package com.zwei.module.iot.product.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.module.iot.product.domain.ProductTsl;
import com.zwei.module.iot.product.service.IProductTslService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 产品物模型定义Controller
 * 
 * @author linx
 * @date 2025-09-05
 */
@Api(tags = "产品物模型管理")
@RestController
@RequestMapping("/iot/product/productTsl")
public class ProductTslController extends BaseController
{
    @Autowired
    private IProductTslService productTslService;

    /**
     * 获取产品物模型定义详细信息
     */
    @ApiOperation("获取产品物模型定义详细信息")
    @PreAuthorize("@ss.hasPermi('iot:productTsl:query')")
    @GetMapping(value = "{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return AjaxResult.success(productTslService.selectProductTslByProductId(id));
    }

    /**
     * 新增产品物模型定义
     */
    @ApiOperation("新增产品物模型定义")
    @PreAuthorize("@ss.hasPermi('iot:productTsl:add')")
    @Log(title = "产品物模型定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ProductTsl productTsl)
    {
        return toAjax(productTslService.insertProductTsl(productTsl));
    }

    /**
     * 修改产品物模型定义
     */
    @ApiOperation("修改产品物模型定义")
    @PreAuthorize("@ss.hasPermi('iot:productTsl:edit')")
    @Log(title = "产品物模型定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody ProductTsl productTsl)
    {
        return toAjax(productTslService.updateProductTsl(productTsl));
    }
}