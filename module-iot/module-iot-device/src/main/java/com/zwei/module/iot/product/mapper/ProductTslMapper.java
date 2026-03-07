package com.zwei.module.iot.product.mapper;

import com.zwei.module.iot.product.domain.ProductTsl;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品物模型定义Mapper接口
 * 
 * @author linx
 * @date 2025-09-05
 */
@Mapper
public interface ProductTslMapper 
{
    /**
     * 查询产品物模型定义
     * 
     * @param productId 产品ID
     * @return 产品物模型定义
     */
    public ProductTsl selectProductTslByProductId(String productId);

    /**
     * 新增产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    public int insertProductTsl(ProductTsl productTsl);

    /**
     * 修改产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    public int updateProductTsl(ProductTsl productTsl);

    /**
     * 删除产品物模型定义
     * 
     * @param productId 产品ID
     * @return 结果
     */
    public int deleteProductTslByProductId(String productId);
}