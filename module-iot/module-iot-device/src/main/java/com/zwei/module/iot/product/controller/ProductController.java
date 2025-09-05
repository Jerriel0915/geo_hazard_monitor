package com.zwei.module.iot.product.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.service.IProductService;
import com.zwei.common.utils.poi.ExcelUtil;
import com.zwei.common.core.page.TableDataInfo;

/**
 * 产品管理Controller
 * 
 * @author zwei
 * @date 2025-09-04
 */
@RestController
@RequestMapping("/product/product")
public class ProductController extends BaseController
{
    @Autowired
    private IProductService productService;

    /**
     * 查询产品管理列表
     */
    @PreAuthorize("@ss.hasPermi('product:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(Product product)
    {
        startPage();
        List<Product> list = productService.selectProductList(product);
        return getDataTable(list);
    }

    /**
     * 导出产品管理列表
     */
    @PreAuthorize("@ss.hasPermi('product:product:export')")
    @Log(title = "产品管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Product product)
    {
        List<Product> list = productService.selectProductList(product);
        ExcelUtil<Product> util = new ExcelUtil<Product>(Product.class);
        util.exportExcel(response, list, "产品管理数据");
    }

    /**
     * 获取产品管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('product:product:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(productService.selectProductById(id));
    }

    /**
     * 新增产品管理
     */
    @PreAuthorize("@ss.hasPermi('product:product:add')")
    @Log(title = "产品管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Product product)
    {
        return toAjax(productService.insertProduct(product));
    }

    /**
     * 修改产品管理
     */
    @PreAuthorize("@ss.hasPermi('product:product:edit')")
    @Log(title = "产品管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Product product)
    {
        return toAjax(productService.updateProduct(product));
    }

    /**
     * 删除产品管理
     */
    @PreAuthorize("@ss.hasPermi('product:product:remove')")
    @Log(title = "产品管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(productService.deleteProductByIds(ids));
    }
}
