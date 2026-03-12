package com.zwei.module.iot.product.mapper;

import com.zwei.module.iot.product.domain.ProductTsl;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

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
    ProductTsl selectProductTslByProductId(String productId);

    /**
     * 批量查询产品物模型定义
     *
     * @param productIds 产品ID集合
     * @return 产品物模型定义列表
     */
    List<ProductTsl> selectProductTslByProductIds(List<String> productIds);

    /**
     * 查询产品物模型定义列表
     *
     * @param productTsl 查询条件
     * @return 产品物模型定义列表
     */
    List<ProductTsl> selectProductTslList(ProductTsl productTsl);

    /**
     * 新增产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    int insertProductTsl(ProductTsl productTsl);

    /**
     * 修改产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    int updateProductTsl(ProductTsl productTsl);

    /**
     * 删除产品物模型定义
     * 
     * @param productId 产品ID
     * @return 结果
     */
    int deleteProductTslByProductId(String productId);
}
