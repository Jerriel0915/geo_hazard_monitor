package com.zwei.module.iot.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwei.iot.core.ThingModel;
import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.mapper.ProductMapper;
import com.zwei.module.iot.product.service.IProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 产品Service业务层处理
 * 
 * @author linx
 * @date 2025-09-05
 */
@Service
public class ProductServiceImpl implements IProductService 
{
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private IDbStructureData dbStructureData;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询产品
     * 
     * @param id 产品主键
     * @return 产品
     */
    @Override
    public Product selectProductById(Long id)
    {
        return productMapper.selectProductById(id);
    }

    /**
     * 查询产品列表
     * 
     * @param product 产品
     * @return 产品
     */
    @Override
    public List<Product> selectProductList(Product product)
    {
        return productMapper.selectProductList(product);
    }

    /**
     * 新增产品
     * 
     * @param product 产品
     * @return 结果
     */
    @Override
    public int insertProduct(Product product)
    {
        int result = productMapper.insertProduct(product);
        
        // 初始化产品的数据表结构
        initProductTables(product);
        
        return result;
    }

    /**
     * 修改产品
     * 
     * @param product 产品
     * @return 结果
     */
    @Override
    public int updateProduct(Product product)
    {
        int result = productMapper.updateProduct(product);
        
        // 更新产品的数据表结构
        initProductTables(product);
        
        return result;
    }

    /**
     * 批量删除产品
     * 
     * @param ids 需要删除的产品主键
     * @return 结果
     */
    @Override
    public int deleteProductByIds(Long[] ids)
    {
        return productMapper.deleteProductByIds(ids);
    }

    /**
     * 删除产品信息
     * 
     * @param id 产品主键
     * @return 结果
     */
    @Override
    public int deleteProductById(Long id)
    {
        // 这里可以添加删除相关数据表的逻辑
        // 注意：删除数据表需要谨慎操作
        
        return productMapper.deleteProductById(id);
    }
    
    /**
     * 初始化产品的数据表结构
     */
    private void initProductTables(Product product) {
        try {
            // 创建基础的物模型对象
            ThingModel thingModel = new ThingModel(product.getProductKey());
            
            // 调用数据库结构服务创建表
            dbStructureData.defineThingModel(thingModel);
            
            System.out.println("成功为产品 " + product.getProductKey() + " 创建数据表结构");
        } catch (Exception e) {
            System.err.println("为产品创建数据表结构失败: " + e.getMessage());
            // 记录异常但不影响产品的创建/更新流程
        }
    }
}
