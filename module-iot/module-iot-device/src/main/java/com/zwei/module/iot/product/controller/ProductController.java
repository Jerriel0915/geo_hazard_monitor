package com.zwei.module.iot.product.controller;

import com.zwei.common.annotation.Log;
import com.zwei.common.core.controller.BaseController;
import com.zwei.common.core.domain.AjaxResult;
import com.zwei.common.core.page.TableDataInfo;
import com.zwei.common.enums.BusinessType;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.service.IProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品Controller
 * 
 * @author linx
 * @date 2025-09-05
 */
@Api(tags = "产品管理")
@RestController
@RequestMapping("/iot/product/product")
public class ProductController extends BaseController
{
    private final IProductService productService;

    @Autowired
    ProductController(IProductService productService) {
        this.productService = productService;
    }

    /**
     * 查询产品列表
     */
    @ApiOperation("查询产品列表")
    @PreAuthorize("@ss.hasPermi('iot:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(Product product)
    {
        startPage();
        List<Product> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 导出产品列表
     */
    @ApiOperation("导出产品列表")
    @PreAuthorize("@ss.hasPermi('iot:product:export')")
    @Log(title = "产品", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(Product product)
    {
        List<Product> list = productService.selectProductList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        return util.exportExcel(list, "产品数据");
    }

    /**
     * 获取产品详细信息
     */
    @ApiOperation("获取产品详细信息")
    @PreAuthorize("@ss.hasPermi('iot:product:query')")
    @GetMapping(value = "{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return AjaxResult.success(productService.selectProductById(id));
    }

    /**
     * 新增产品
     */
    @ApiOperation("新增产品")
    @PreAuthorize("@ss.hasPermi('iot:product:add')")
    @Log(title = "产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product)
    {
        return toAjax(productService.insertProduct(product));
    }

    /**
     * 修改产品
     */
    @ApiOperation("修改产品")
    @PreAuthorize("@ss.hasPermi('iot:product:edit')")
    @Log(title = "产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product)
    {
        return toAjax(productService.updateProduct(product));
    }

    /**
     * 删除产品
     */
    @ApiOperation("删除产品")
    @PreAuthorize("@ss.hasPermi('iot:product:remove')")
    @Log(title = "产品", businessType = BusinessType.DELETE)
	@DeleteMapping("{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(productService.deleteProductByIds(ids));
    }
}
