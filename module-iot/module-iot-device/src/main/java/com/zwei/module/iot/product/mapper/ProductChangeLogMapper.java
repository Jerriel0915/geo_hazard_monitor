package com.zwei.module.iot.product.mapper;

import com.zwei.module.iot.product.domain.ProductChangeLog;

import java.util.List;

/**
 * 产品变更日志Mapper接口
 * 
 * @author linx
 * @date 2025-09-05
 */
public interface ProductChangeLogMapper {
    /**
     * 新增产品变更日志
     * 
     * @param productChangeLog 产品变更日志
     * @return 结果
     */
    int insertProductChangeLog(ProductChangeLog productChangeLog);

    /**
     * 更新产品变更日志
     * 
     * @param productChangeLog 产品变更日志
     * @return 结果
     */
    int updateProductChangeLog(ProductChangeLog productChangeLog);

    /**
     * 查询产品变更日志列表
     * 
     * @param productChangeLog 产品变更日志
     * @return 产品变更日志集合
     */
    List<ProductChangeLog> selectProductChangeLogList(ProductChangeLog productChangeLog);

    /**
     * 根据产品ID查询变更日志
     * 
     * @param productId 产品ID
     * @return 变更日志列表
     */
    List<ProductChangeLog> selectProductChangeLogsByProductId(String productId);
}