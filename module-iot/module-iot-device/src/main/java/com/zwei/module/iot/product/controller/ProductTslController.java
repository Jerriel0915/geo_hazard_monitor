package com.zwei.module.iot.product.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.enums.BusinessType;
import com.zwei.module.iot.product.domain.ProductTsl;
import com.zwei.module.iot.product.service.IProductTslService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

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
    @GetMapping(value = "{productId}")
    public AjaxResult getInfo(@PathVariable("productId") String productId)
    {
        return AjaxResult.success(productTslService.selectProductTslByProductId(productId));
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

    /**
     * 删除产品物模型定义
     */
    @ApiOperation("删除产品物模型定义")
    @PreAuthorize("@ss.hasPermi('iot:productTsl:remove')")
    @Log(title = "产品物模型定义", businessType = BusinessType.DELETE)
	@DeleteMapping("{productId}")
    public AjaxResult remove(@PathVariable String productId)
    {
        return toAjax(productTslService.deleteProductTslByProductId(productId));
    }
}