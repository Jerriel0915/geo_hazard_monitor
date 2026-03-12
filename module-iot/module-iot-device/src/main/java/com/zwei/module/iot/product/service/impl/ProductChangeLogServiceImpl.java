//package com.zwei.module.iot.product.service.impl;
//
//import com.alibaba.fastjson2.JSONObject;
//import com.zwei.iot.storage.core.IDbStructureData;
//import com.zwei.module.iot.product.domain.ProductChangeLog;
//import com.zwei.module.iot.product.mapper.ProductChangeLogMapper;
//import com.zwei.module.iot.product.service.IProductChangeLogService;
//import com.zwei.module.iot.thing.domain.ThingModel;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//import java.util.Date;
//import java.util.List;
//
/// **
// * 产品变更日志Service业务层处理
// *
// * @author linx
// * @date 2025-09-05
// */
//@Service
//@Slf4j
//public class ProductChangeLogServiceImpl implements IProductChangeLogService {
//    @Autowired
//    private ProductChangeLogMapper productChangeLogMapper;
//
//    @Autowired
//    private IDbStructureData dbStructureData;
//
//    /**
//     * 新增产品变更日志
//     */
//    @Override
//    public int insertProductChangeLog(ProductChangeLog productChangeLog) {
//        return productChangeLogMapper.insertProductChangeLog(productChangeLog);
//    }
//
//    /**
//     * 更新产品变更日志
//     */
//    @Override
//    public int updateProductChangeLog(ProductChangeLog productChangeLog) {
//        return productChangeLogMapper.updateProductChangeLog(productChangeLog);
//    }
//
//    /**
//     * 查询产品变更日志列表
//     */
//    @Override
//    public List<ProductChangeLog> selectProductChangeLogList(ProductChangeLog productChangeLog) {
//        return productChangeLogMapper.selectProductChangeLogList(productChangeLog);
//    }
//
//    /**
//     * 根据产品ID查询变更日志
//     */
//    @Override
//    public List<ProductChangeLog> selectProductChangeLogsByProductId(String productId) {
//        return productChangeLogMapper.selectProductChangeLogsByProductId(productId);
//    }
//
//    /**
//     * 记录物模型变更日志并异步执行表结构更新
//     */
//    @Override
//    public void recordTslChangeLog(Long productId, String productKey, String tslContent, Integer operationType) {
//        // 创建变更日志记录
//        ProductChangeLog log = new ProductChangeLog();
//        log.setProductId(productId);
//        log.setProductKey(productKey);
//        log.setOperationType(operationType);
//        log.setStatus(0); // 0-待执行
//        log.setTslContent(tslContent);
//        log.setCreateTime(new Date());
//
//        // 保存日志
//        productChangeLogMapper.insertProductChangeLog(log);
//
//        // 异步执行表结构更新
//        executeUpdateDbStructureAsync(log);
//    }
//
//    /**
//     * 异步执行数据库表结构更新
//     */
//    @Async
//    protected void executeUpdateDbStructureAsync(ProductChangeLog changelog) {
//        try {
//            // 解析物模型JSON字符串
//            ThingModel thingModel = new ThingModel(changelog.getProductKey());
//            Model model = JSONObject.parseObject(changelog.getTslContent(), Model.class);
//            thingModel.setModel(model);
//
//            // 更新数据库表结构
//            dbStructureData.updateThingModel(thingModel);
//
//            // 更新日志状态为成功
//            changelog.setStatus(1); // 1-执行成功
//            changelog.setExecuteTime(new Date());
//            productChangeLogMapper.updateProductChangeLog(changelog);
//
//            log.info("异步更新产品 {} 的数据表结构成功", changelog.getProductId());
//        } catch (Exception e) {
//            // 更新日志状态为失败
//            changelog.setStatus(2); // 2-执行失败
//            changelog.setErrorMessage(e.getMessage());
//            changelog.setExecuteTime(new Date());
//            productChangeLogMapper.updateProductChangeLog(changelog);
//
//            log.error("异步更新产品 {} 的数据表结构失败: {}", changelog.getProductId(), e.getMessage());
//        }
//    }
//}