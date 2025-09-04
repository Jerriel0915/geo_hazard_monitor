package com.zwei.module.iot.product.mapper;

import java.util.List;
import com.zwei.module.iot.product.domain.Product;

/**
 * 产品管理Mapper接口
 * 
 * @author zwei
 * @date 2025-09-04
 */
public interface ProductMapper 
{
    /**
     * 查询产品管理
     * 
     * @param id 产品管理主键
     * @return 产品管理
     */
    public Product selectProductById(Long id);

    /**
     * 查询产品管理列表
     * 
     * @param product 产品管理
     * @return 产品管理集合
     */
    public List<Product> selectProductList(Product product);

    /**
     * 新增产品管理
     * 
     * @param product 产品管理
     * @return 结果
     */
    public int insertProduct(Product product);

    /**
     * 修改产品管理
     * 
     * @param product 产品管理
     * @return 结果
     */
    public int updateProduct(Product product);

    /**
     * 删除产品管理
     * 
     * @param id 产品管理主键
     * @return 结果
     */
    public int deleteProductById(Long id);

    /**
     * 批量删除产品管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteProductByIds(Long[] ids);
}
