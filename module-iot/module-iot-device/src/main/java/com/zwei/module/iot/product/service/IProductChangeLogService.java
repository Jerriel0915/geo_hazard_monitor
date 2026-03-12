//package com.zwei.module.iot.product.service;
//
//import com.zwei.module.iot.product.domain.ProductChangeLog;
//import java.util.List;
//
//import org.apache.commons.math3.analysis.function.Log;
//
/// **
// * 产品变更日志Service接口
// *
// * @author linx
// * @date 2025-09-05
// */
//public interface IProductChangeLogService {
//    /**
//     * 新增产品变更日志
//     *
//     * @param productChangeLog 产品变更日志
//     * @return 结果
//     */
//    public int insertProductChangeLog(ProductChangeLog productChangeLog);
//
//    /**
//     * 更新产品变更日志
//     *
//     * @param productChangeLog 产品变更日志
//     * @return 结果
//     */
//    public int updateProductChangeLog(ProductChangeLog productChangeLog);
//
//    /**
//     * 查询产品变更日志列表
//     *
//     * @param productChangeLog 产品变更日志
//     * @return 产品变更日志集合
//     */
//    public List<ProductChangeLog> selectProductChangeLogList(ProductChangeLog productChangeLog);
//
//    /**
//     * 根据产品ID查询变更日志
//     *
//     * @param productId 产品ID
//     * @return 变更日志列表
//     */
//    public List<ProductChangeLog> selectProductChangeLogsByProductId(String productId);
//
//    /**
//     * 记录物模型变更日志并异步执行表结构更新
//     *
//     * @param productId 产品ID
//     * @param productKey 产品密钥
//     * @param tslContent 物模型内容
//     * @param operationType 操作类型
//     */
//    public void recordTslChangeLog(Long productId, String productKey, String tslContent, Integer operationType);
//}