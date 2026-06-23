package com.zwei.iot.monitor.service;

import com.zwei.iot.monitor.domain.MonitorContent;

import java.util.List;

/**
 * 监测内容Service接口
 * <p>
 * 定义监测内容的管理操作，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
public interface IMonitorContentService {

    /**
     * 查询监测内容列表
     *
     * @param monitorContent 查询条件
     * @return 监测内容列表
     */
    List<MonitorContent> selectMonitorContentList(MonitorContent monitorContent);

    /**
     * 查询所有监测内容列表（不分页）
     *
     * @param monitorTypeId 监测类型ID（可选）
     * @return 监测内容列表
     */
    List<MonitorContent> selectMonitorContentAll(Long monitorTypeId);

    /**
     * 根据ID查询监测内容详情
     *
     * @param id 监测内容ID
     * @return 监测内容详情
     */
    MonitorContent selectMonitorContentById(Long id);

    /**
     * 新增监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    int insertMonitorContent(MonitorContent monitorContent);

    /**
     * 修改监测内容
     *
     * @param monitorContent 监测内容信息
     * @return 影响行数
     */
    int updateMonitorContent(MonitorContent monitorContent);

    /**
     * 删除监测内容（逻辑删除）
     *
     * @param id 监测内容ID
     * @return 影响行数
     */
    int deleteMonitorContentById(Long id);

    /**
     * 按监测类型删除监测内容（逻辑删除）
     *
     * @param monitorTypeId 监测类型ID
     * @return 影响行数
     */
    int deleteMonitorContentByMonitorTypeId(Long monitorTypeId);

    /**
     * 批量删除监测内容（逻辑删除）
     *
     * @param ids 需要删除的监测内容ID数组
     * @return 影响行数
     */
    int deleteMonitorContentByIds(Long[] ids);

    /**
     * 按监测类型批量删除监测内容（逻辑删除）
     *
     * @param monitorTypeIds 监测类型ID数组
     * @return 影响行数
     */
    int deleteMonitorContentByMonitorTypeIds(Long[] monitorTypeIds);

    /**
     * 校验监测内容编码是否唯一
     *
     * @param monitorContent 监测内容信息
     * @return true-唯一，false-已存在
     */
    boolean checkMonitorContentCodeUnique(MonitorContent monitorContent);

    /**
     * 查询指定监测类型下的所有计算属性(按 sort_order 排序)。
     * 供 ComputedAttributeRegistry 使用。
     *
     * @param monitorTypeId 监测类型ID
     * @return 计算属性列表, 空列表表示无计算属性
     */
    List<MonitorContent> selectComputedByTypeId(Long monitorTypeId);
}
