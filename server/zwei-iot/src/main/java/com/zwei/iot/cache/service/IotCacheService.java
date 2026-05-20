package com.zwei.iot.cache.service;

import com.zwei.iot.hazardpoint.domain.HazardPoint;
import com.zwei.iot.hazardpoint.domain.HazardPointGroup;
import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;

import java.util.List;

/**
 * IoT缓存服务接口
 * <p>
 * 提供业务数据的Redis缓存存取操作
 *
 * @author zwei
 */
public interface IotCacheService {
    // ==================== HazardPoint 缓存 ====================

    /**
     * 缓存隐患点
     */
    void cacheHazardPoint(HazardPoint point);

    /**
     * 批量缓存隐患点
     */
    void cacheHazardPointList(List<HazardPoint> points);

    /**
     * 获取缓存的隐患点
     */
    HazardPoint getHazardPoint(Long id);

    /**
     * 批量获取缓存的隐患点
     */
    List<HazardPoint> getHazardPointList(List<Long> ids);

    /**
     * 删除隐患点缓存
     */
    void evictHazardPoint(Long id);

    /**
     * 批量删除隐患点缓存
     */
    void evictHazardPointList(Long[] ids);

    /**
     * 清空隐患点缓存
     */
    void clearHazardPointCache();

    // ==================== MonitorType 缓存 ====================

    /**
     * 缓存监测类型
     */
    void cacheMonitorType(MonitorType monitorType);

    /**
     * 批量缓存监测类型
     */
    void cacheMonitorTypeList(List<MonitorType> monitorTypes);

    /**
     * 获取缓存的监测类型
     */
    MonitorType getMonitorType(Long id);

    /**
     * 批量获取缓存的监测类型
     */
    List<MonitorType> getMonitorTypeList(List<Long> ids);

    /**
     * 删除监测类型缓存
     */
    void evictMonitorType(Long id);

    /**
     * 批量删除监测类型缓存
     */
    void evictMonitorTypeList(Long[] ids);

    /**
     * 清空监测类型缓存
     */
    void clearMonitorTypeCache();

    // ==================== MonitorContent 缓存 ====================

    /**
     * 缓存监测内容
     */
    void cacheMonitorContent(MonitorContent content);

    /**
     * 批量缓存监测内容
     */
    void cacheMonitorContentList(List<MonitorContent> contents);

    /**
     * 获取缓存的监测内容
     */
    MonitorContent getMonitorContent(Long id);

    /**
     * 批量获取缓存的监测内容
     */
    List<MonitorContent> getMonitorContentList(List<Long> ids);

    /**
     * 删除监测内容缓存
     */
    void evictMonitorContent(Long id);

    /**
     * 批量删除监测内容缓存
     */
    void evictMonitorContentList(Long[] ids);

    /**
     * 清空监测内容缓存
     */
    void clearMonitorContentCache();

    // ==================== HazardPointGroup 缓存 ====================

    /**
     * 缓存隐患点分组
     */
    void cacheHazardPointGroup(HazardPointGroup group);

    /**
     * 批量缓存隐患点分组
     */
    void cacheHazardPointGroupList(List<HazardPointGroup> groups);

    /**
     * 获取缓存的隐患点分组
     */
    HazardPointGroup getHazardPointGroup(Long id);

    /**
     * 批量获取缓存的隐患点分组
     */
    List<HazardPointGroup> getHazardPointGroupList(List<Long> ids);

    /**
     * 删除隐患点分组缓存
     */
    void evictHazardPointGroup(Long id);

    /**
     * 批量删除隐患点分组缓存
     */
    void evictHazardPointGroupList(Long[] ids);

    /**
     * 清空隐患点分组缓存
     */
    void clearHazardPointGroupCache();

    // ==================== 全量清空 ====================

    /**
     * 清空所有IoT业务缓存
     */
    void clearAllCache();
}