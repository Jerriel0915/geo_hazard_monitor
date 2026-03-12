package com.zwei.module.iot.product.service;

import com.zwei.module.iot.product.domain.Product;

import java.util.List;

/**
 * 产品Service接口
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface IProductService 
{
    /**
     * 查询产品
     * 
     * @param id 产品主键
     * @return 产品
     */
    Product selectProductById(Long id);

    /**
     * 查询产品列表
     * 
     * @param product 产品
     * @return 产品集合
     */
    List<Product> selectProductList(Product product);

    /**
     * 新增产品
     * 
     * @param product 产品
     * @return 结果
     */
    int insertProduct(Product product);

    /**
     * 修改产品
     * 
     * @param product 产品
     * @return 结果
     */
    int updateProduct(Product product);

    /**
     * 批量删除产品
     * 
     * @param ids 需要删除的产品主键集合
     * @return 结果
     */
    int deleteProductByIds(Long[] ids);

    /**
     * 删除产品信息
     * 
     * @param id 产品主键
     * @return 结果
     */
    int deleteProductById(Long id);
}
