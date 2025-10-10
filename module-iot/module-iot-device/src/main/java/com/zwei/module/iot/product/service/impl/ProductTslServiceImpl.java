package com.zwei.module.iot.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zwei.module.iot.product.mapper.ProductTslMapper;
import com.zwei.module.iot.product.domain.ProductTsl;
import com.zwei.module.iot.product.service.IProductTslService;

/**
 * 产品物模型定义Service业务层处理
 * 
 * @author linx
 * @date 2025-09-05
 */
@Service
public class ProductTslServiceImpl implements IProductTslService 
{
    @Autowired
    private ProductTslMapper productTslMapper;

    /**
     * 查询产品物模型定义
     * 
     * @param productId 产品ID
     * @return 产品物模型定义
     */
    @Override
    public ProductTsl selectProductTslByProductId(String productId)
    {
        return productTslMapper.selectProductTslByProductId(productId);
    }

    /**
     * 新增产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    public int insertProductTsl(ProductTsl productTsl)
    {
        return productTslMapper.insertProductTsl(productTsl);
    }

    /**
     * 修改产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    public int updateProductTsl(ProductTsl productTsl)
    {
        return productTslMapper.updateProductTsl(productTsl);
    }

    /**
     * 删除产品物模型定义
     * 
     * @param productId 产品ID
     * @return 结果
     */
    @Override
    public int deleteProductTslByProductId(String productId)
    {
        return productTslMapper.deleteProductTslByProductId(productId);
    }
}