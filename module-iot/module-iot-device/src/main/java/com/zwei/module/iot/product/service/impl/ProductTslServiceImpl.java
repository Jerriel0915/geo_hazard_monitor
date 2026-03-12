package com.zwei.module.iot.product.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.zwei.common.core.redis.RedisCache;
import com.zwei.framework.manager.CacheWarmupTask;
import com.zwei.iot.core.thing.domain.TslProperty;
import com.zwei.iot.storage.core.IDbStructureData;
import com.zwei.module.iot.product.domain.Product;
import com.zwei.module.iot.product.domain.ProductTsl;
import com.zwei.module.iot.product.mapper.ProductMapper;
import com.zwei.module.iot.product.mapper.ProductTslMapper;
import com.zwei.module.iot.product.service.IProductTslService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 产品物模型定义Service业务层处理
 *
 * @author linx
 * @date 2025-09-05
 */
@Service
@Slf4j
public class ProductTslServiceImpl implements IProductTslService, CacheWarmupTask {
    private final ProductMapper productMapper;
    private final ProductTslMapper productTslMapper;
    private final IDbStructureData dbStructureData;
    private final RedisCache redisCache;

    private final Random random = new Random();

    private static final String CACHE_KEY_PREFIX = "iot:product:tsl:";
    private static final long EXPIRE_SECONDS = 60 * 60 * 12L;

//    @Autowired
//    private IProductChangeLogService productChangeLogService;

    @Autowired
    ProductTslServiceImpl(ProductMapper productMapper, ProductTslMapper productTslMapper, RedisCache redisCache, IDbStructureData dbStructureData) {
        this.productMapper = productMapper;
        this.productTslMapper = productTslMapper;
        this.redisCache = redisCache;
        this.dbStructureData = dbStructureData;
    }

    @Override
    public String getTaskName() {
        return "ProductTslService";
    }

    @Override
    public void warmup() throws InterruptedException {
        int pageSize = 1_000;
        int pageNo = 1;
        long total = 0;

        while (true) {
            PageHelper.startPage(pageNo, pageSize);
            List<ProductTsl> productTsls = productTslMapper.selectProductTslList(new ProductTsl());

            if (productTsls == null || productTsls.isEmpty()) {
                break;
            }

            redisCache.redisTemplate.executePipelined((RedisCallback<?>) connections -> {
                productTsls.forEach(productTsl -> {
                    if (productTsl == null || productTsl.getProductId() == null) {
                        return;
                    }

                    byte[] key = (CACHE_KEY_PREFIX + productTsl.getProductId()).getBytes();
                    byte[] value = JSONObject.toJSONString(productTsl).getBytes();
                    long expire = EXPIRE_SECONDS + random.nextLong() % 7200;

                    connections.setEx(key, expire, value);
                });
                return null;
            });

            total += productTsls.size();
            pageNo++;

            if (pageNo % 10 == 0) {
                Thread.sleep(100);
            }
        }

        log.info("产品物模型缓存预热结束，总量: {}", total);
    }

    /**
     * 查询产品物模型定义
     *
     * @param productId 产品ID
     * @return 产品物模型定义
     */
    @Override
    public ProductTsl selectProductTslByProductId(String productId) {
        String cacheKey = CACHE_KEY_PREFIX + productId;
        ProductTsl productTsl = redisCache.getCacheObject(cacheKey);
        if (productTsl != null) {
            return productTsl;
        }
        productTsl = productTslMapper.selectProductTslByProductId(productId);
        if (productTsl != null) {
            redisCache.setCacheObject(cacheKey, productTsl, 1, TimeUnit.HOURS);
        }
        return productTsl;
    }

    @Override
    public List<ProductTsl> selectProductTslByProductIds(List<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<>(productIds.size());
        for (String productId : productIds) {
            keys.add(CACHE_KEY_PREFIX + productId);
        }

        List<Object> cachedValues = redisCache.redisTemplate.opsForValue().multiGet(keys);

        Map<String, ProductTsl> resultMap = new HashMap<>();
        Set<String> missIdSet = new LinkedHashSet<>();

        for (int i = 0; i < productIds.size(); i++) {
            Object cachedValue = (cachedValues == null || cachedValues.size() <= i) ? null : cachedValues.get(i);
            if (cachedValue instanceof ProductTsl) {
                resultMap.put(productIds.get(i), (ProductTsl) cachedValue);
            } else {
                missIdSet.add(productIds.get(i));
            }
        }

        if (!missIdSet.isEmpty()) {
            List<String> missIds = new ArrayList<>(missIdSet);
            List<ProductTsl> dbValues = productTslMapper.selectProductTslByProductIds(missIds);

            if (dbValues != null && !dbValues.isEmpty()) {
                redisCache.redisTemplate.executePipelined((RedisCallback<?>) connections -> {
                    dbValues.forEach(productTsl -> {
                        if (productTsl == null || productTsl.getProductId() == null) {
                            return;
                        }

                        byte[] key = (CACHE_KEY_PREFIX + productTsl.getProductId()).getBytes();
                        byte[] value = redisCache.redisTemplate.getValueSerializer().serialize(productTsl);
                        long expire = EXPIRE_SECONDS + random.nextInt(600);

                        connections.setEx(key, expire, value);
                    });
                    return null;
                });

                for (ProductTsl productTsl : dbValues) {
                    if (productTsl != null && productTsl.getProductId() != null) {
                        resultMap.put(String.valueOf(productTsl.getProductId()), productTsl);
                    }
                }
            }
        }

        List<ProductTsl> results = new ArrayList<>(productIds.size());
        for (String productId : productIds) {
            ProductTsl productTsl = resultMap.get(productId);
            if (productTsl != null) {
                results.add(productTsl);
            }
        }
        return results;
    }

    @Override
    public List<TslProperty> selectTslPropertyByProductId(String productId) {
        ProductTsl productTsl = selectProductTslByProductId(productId);
        if (productTsl != null) {
            return productTsl.getTsl().getProperties();
        }
        return Collections.emptyList();
    }

    /**
     * 新增产品物模型定义
     *
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    @Transactional
    public int insertProductTsl(ProductTsl productTsl) {
        int result = productTslMapper.insertProductTsl(productTsl);

        // 根据物模型定义更新数据库表结构
        updateDbStructureFromTsl(productTsl);

        if (result > 0 && productTsl.getProductId() != null) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + productTsl.getProductId());
        }

        return result;
    }

    /**
     * 修改产品物模型定义
     *
     * @param productTsl 产品物模型定义
     * @return 结果
     */
    @Override
    @Transactional
    public int updateProductTsl(ProductTsl productTsl) {
        int result = productTslMapper.updateProductTsl(productTsl);

        // 根据物模型定义更新数据库表结构
        updateDbStructureFromTsl(productTsl);

        if (result > 0 && productTsl.getProductId() != null) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + productTsl.getProductId());
        }

        return result;
    }

    /**
     * 删除产品物模型定义
     *
     * @param productId 产品ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteProductTslByProductId(String productId) {
        // 这里可以添加清理相关数据表结构的逻辑
        // 注意：删除数据表需要谨慎操作

        int result = productTslMapper.deleteProductTslByProductId(productId);
        if (result > 0) {
            redisCache.deleteObject(CACHE_KEY_PREFIX + productId);
        }
        return result;
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
                ProductTsl existingTsl = productTslMapper.selectProductTslByProductId(productTsl.getProductId().toString());
                if (existingTsl != null) {
                    operationType = 1; // 更新操作
                }

//                // 记录变更日志并异步执行表结构更新
//                productChangeLogService.recordTslChangeLog(
//                    productTsl.getProductId(),
//                    product.getProductKey(),
//                    productTsl.getTsl(),
//                    operationType
//                );

                switch (operationType) {
                    case 0:
                        productTslMapper.insertProductTsl(productTsl);
                        dbStructureData.defineThingModel(productTsl.getTsl());
                        break;
                    case 1:
                        productTslMapper.updateProductTsl(productTsl);
                        dbStructureData.updateThingModel(productTsl.getTsl());
                        break;
                }

                log.info("已提交产品 {} 的物模型变更，内容为 {}",
                        productTsl.getProductId(),
                        productTsl.getTsl());
            }
        } catch (Exception e) {
            log.error("提交物模型变更失败: {}", e.getMessage());
            // 即使提交失败，也不影响主流程，但需要提示用户查看变更日志
        }
    }
}
