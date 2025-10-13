package com.zwei.module.iot.product.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.zwei.iot.core.ThingModel;
import com.zwei.iot.core.ThingModel.Model;
import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.domain.ProductTsl;
import com.zwei.module.iot.product.mapper.ProductMapper;
import com.zwei.module.iot.product.mapper.ProductTslMapper;
import com.zwei.module.iot.product.service.IProductChangeLogService;
import com.zwei.module.iot.product.service.IProductTslService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 产品物模型定义Service业务层处理
 * 
 * @author linx
 * @date 2025-09-05
 */
@Service
@Slf4j
public class ProductTslServiceImpl implements IProductTslService {
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductTslMapper productTslMapper;

    @Autowired
    private IDbStructureData dbStructureData;

    @Autowired
    private IProductChangeLogService productChangeLogService;

    /**
     * 查询产品物模型定义
     * 
     * @param productId 产品ID
     * @return 产品物模型定义
     */
    @Override
    public ProductTsl selectProductTslByProductId(Long productId) {
        return productTslMapper.selectProductTslByProductId(productId);
    }

    /**
     * 新增产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    public int insertProductTsl(ProductTsl productTsl) {
        int result = productTslMapper.insertProductTsl(productTsl);

        // 根据物模型定义更新数据库表结构
        updateDbStructureFromTsl(productTsl);

        return result;
    }

    /**
     * 修改产品物模型定义
     * 
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    public int updateProductTsl(ProductTsl productTsl) {
        int result = productTslMapper.updateProductTsl(productTsl);

        // 根据物模型定义更新数据库表结构
        updateDbStructureFromTsl(productTsl);

        return result;
    }

    /**
     * 删除产品物模型定义
     * 
     * @param productId 产品ID
     * @return 结果
     */
    @Override
    public int deleteProductTslByProductId(String productId) {
        // 这里可以添加清理相关数据表结构的逻辑
        // 注意：删除数据表需要谨慎操作

        return productTslMapper.deleteProductTslByProductId(productId);
    }

    /**
     * 根据物模型定义更新数据库表结构
     * 
     * @param productTsl 产品物模型定义
     */
    private void updateDbStructureFromTsl(ProductTsl productTsl) {
        try {
            if (productTsl != null && productTsl.getProductId() != null && productTsl.getTsl() != null) {
                Product product = productMapper.selectProductById(productTsl.getProductId());
                if (product == null) {
                    log.error("产品 {} 不存在", productTsl.getProductId());
                    return;
                }

                // 检查是否已存在该产品的物模型，判断是新增还是更新操作
                Integer operationType = 0; // 默认为新增操作
                ProductTsl existingTsl = productTslMapper.selectProductTslByProductId(productTsl.getProductId());
                if (existingTsl != null) {
                    operationType = 1; // 更新操作
                }

                // 记录变更日志并异步执行表结构更新
                productChangeLogService.recordTslChangeLog(
                    productTsl.getProductId(),
                    product.getProductKey(),
                    productTsl.getTsl(),
                    operationType
                );

                log.info("已提交产品 {} 的物模型变更，正在异步执行表结构更新，请稍后查看产品变更日志确认执行结果", 
                    productTsl.getProductId());
            }
        } catch (Exception e) {
            log.error("提交物模型变更失败: {}", e.getMessage());
            // 即使提交失败，也不影响主流程，但需要提示用户查看变更日志
        }
    }
}