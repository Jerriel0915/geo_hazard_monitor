package com.zwei.iot.mapper;

import com.zwei.iot.domain.MonitorContent;
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
     * 批量删除监测内容（逻辑删除）
     *
     * @param ids 需要删除的监测内容ID数组
     * @return 影响行数
     */
    int deleteMonitorContentByIds(Long[] ids);

    /**
     * 校验监测内容编码是否唯一
     *
     * @param code 监测内容编码
     * @param id   排除的监测内容ID（更新时使用）
     * @return 监测内容信息（null表示唯一）
     */
    MonitorContent checkMonitorContentCodeUnique(@Param("code") String code, @Param("id") Long id);
}