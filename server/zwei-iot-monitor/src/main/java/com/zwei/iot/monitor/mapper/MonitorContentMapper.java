package com.zwei.iot.monitor.mapper;

import com.zwei.iot.monitor.domain.MonitorContent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 监测内容Mapper接口
 * <p>
 * 提供监测内容的数据库查询操作，包括列表查询、详情查询、
 * 新增、修改、删除等CRUD操作。
 *
 * @author zwei
 */
@Mapper
public interface MonitorContentMapper {

    /**
     * 查询监测内容列表
     *
     * @param monitorContent 监测内容查询条件
     * @return 监测内容列表
     */
    List<MonitorContent> selectMonitorContentList(MonitorContent monitorContent);

    /**
     * 查询所有监测内容列表（不分页）
     *
     * @param monitorTypeId 监测类型ID（可选，用于过滤）
     * @return 监测内容列表
     */
    List<MonitorContent> selectMonitorContentAll(@Param("monitorTypeId") Long monitorTypeId);

    /**
     * 根据ID查询监测内容详情
     *
     * @param id 监测内容ID
     * @return 监测内容详情
     */
    MonitorContent selectMonitorContentById(Long id);

    /**
     * 根据编码查询监测内容
     *
     * @param code 监测内容编码
     * @return 监测内容详情
     */
    MonitorContent selectMonitorContentByCode(String code);

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
     * @param code 监测内容编码
     * @param id   排除的监测内容ID（更新时使用）
     * @return 监测内容信息（null表示唯一）
     */
    MonitorContent checkMonitorContentCodeUnique(@Param("code") String code, @Param("id") Long id);

    /**
     * 查询指定监测类型下最大的 sort_order
     *
     * @param monitorTypeId 监测类型ID
     * @return 最大 sort_order（无数据时返回 0）
     */
    Integer selectMaxSortOrderByMonitorTypeId(@Param("monitorTypeId") Long monitorTypeId);

    /**
     * 检查指定监测类型下 sort_order 是否已被其他行占用
     *
     * @param monitorTypeId 监测类型ID
     * @param sortOrder    sort_order 值
     * @param excludeId    排除自身ID（更新时使用，插入时传 null）
     * @return 冲突的监测内容（null 表示唯一）
     */
    MonitorContent checkSortOrderExists(@Param("monitorTypeId") Long monitorTypeId,
                                         @Param("sortOrder") Integer sortOrder,
                                         @Param("excludeId") Long excludeId);
}
