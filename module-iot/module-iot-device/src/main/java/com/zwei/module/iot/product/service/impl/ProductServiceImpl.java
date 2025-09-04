package com.zwei.module.iot.product.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.module.iot.product.mapper.ProductMapper;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.service.IProductService;

/**
 * 产品管理Service业务层处理
 * 
 * @author zwei
 * @date 2025-09-04
 */
@Service
public class ProductServiceImpl implements IProductService 
{
    @Autowired
    private ProductMapper productMapper;

    /**
     * 查询产品管理
     * 
     * @param id 产品管理主键
     * @return 产品管理
     */
    @Override
    public Product selectProductById(Long id)
    {
        return productMapper.selectProductById(id);
    }

    /**
     * 查询产品管理列表
     * 
     * @param product 产品管理
     * @return 产品管理
     */
    @Override
    public List<Product> selectProductList(Product product)
    {
        return productMapper.selectProductList(product);
    }

    /**
     * 新增产品管理
     * 
     * @param product 产品管理
     * @return 结果
     */
    @Override
    public int insertProduct(Product product)
    {
        return productMapper.insertProduct(product);
    }

    /**
     * 修改产品管理
     * 
     * @param product 产品管理
     * @return 结果
     */
    @Override
    public int updateProduct(Product product)
    {
        return productMapper.updateProduct(product);
    }

    /**
     * 批量删除产品管理
     * 
     * @param ids 需要删除的产品管理主键
     * @return 结果
     */
    @Override
    public int deleteProductByIds(Long[] ids)
    {
        return productMapper.deleteProductByIds(ids);
    }

    /**
     * 删除产品管理信息
     * 
     * @param id 产品管理主键
     * @return 结果
     */
    @Override
    public int deleteProductById(Long id)
    {
        return productMapper.deleteProductById(id);
    }
}
