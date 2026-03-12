package com.zwei.module.iot.product.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.framework.manager.CacheWarmupTask;
import com.zwei.iot.core.thing.domain.ThingModel;
import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.mapper.ProductMapper;
import com.zwei.module.iot.product.service.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 产品Service业务层处理
 *
 * @author linx
 * @date 2025-09-05
 */
@Service
@Slf4j
public class ProductServiceImpl implements IProductService, CacheWarmupTask {
    private final ProductMapper productMapper;
    private final IDbStructureData dbStructureData;
    private final RedisCache redisCache;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Random random = new Random();

    private static final String CACHE_KEY_PREFIX = "iot:product:id:";
    private static final Long EXPIRE_SECONDS = 60 * 60 * 12L;

    @Autowired
    ProductServiceImpl(ProductMapper productMapper, RedisCache redisCache, IDbStructureData dbStructureData) {
        this.productMapper = productMapper;
        this.redisCache = redisCache;
        this.dbStructureData = dbStructureData;
    }

    @Override
    public String getTaskName() {
        return "ProductService";
    }

    @Override
    public void warmup() throws InterruptedException {
        int pageSize = 1_000;
        int pageNo = 1;
        long total = 0;

        while (true) {
            PageHelper.startPage(pageNo, pageSize);
            List<Product> products = selectProductList(new Product());

            if (products == null || products.isEmpty()) {
                break;
            }

            redisCache.redisTemplate.executePipelined((RedisCallback<?>) connections -> {
                products.forEach(device -> {
                    byte[] key = (CACHE_KEY_PREFIX + device.getId()).getBytes();
                    byte[] value = JSONObject.toJSONString(device).getBytes();
                    long expire = EXPIRE_SECONDS + random.nextLong() % 7200;

                    connections.setEx(key, expire, value);
                });
                return null;
            });

            total += products.size();
            pageNo++;

            if (pageNo % 10 == 0) {
                Thread.sleep(100);  // 每1万条休息100ms
            }
        }

        log.info("产品缓存预热结束，总量: {}", total);
    }


    /**
     * 查询产品
     *
     * @param id 产品主键
     * @return 产品
     */
    @Override
    public Product selectProductById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        Product product = redisCache.getCacheObject(cacheKey);
        if (product != null) {
            return product;
        }
        product = productMapper.selectProductById(id);
        if (product != null) {
            redisCache.setCacheObject(cacheKey, product, 1, TimeUnit.HOURS);
        }
        return product;
    }

    /**
     * 查询产品列表
     *
     * @param product 产品
     * @return 产品
     */
    @Override
    public List<Product> selectProductList(Product product) {
        return productMapper.selectProductList(product);
    }

    /**
     * 新增产品
     *
     * @param product 产品
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProduct(Product product) {
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
    @Transactional
    public int updateProduct(Product product) {
        int result = productMapper.updateProduct(product);
        if (result > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + product.getId());
        }

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
    @Transactional
    public int deleteProductByIds(Long[] ids) {
        int result = productMapper.deleteProductByIds(ids);
        if (result > 0) {
            for (Long id : ids) {
                redisCache.deleteObject(CACHE_KEY_PREFIX + id);
            }
        }
        return result;
    }

    /**
     * 删除产品信息
     *
     * @param id 产品主键
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteProductById(Long id) {
        // 这里可以添加删除相关数据表的逻辑
        // 注意：删除数据表需要谨慎操作

        int result = productMapper.deleteProductById(id);
        if (result > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + id);
        }
        return result;
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
