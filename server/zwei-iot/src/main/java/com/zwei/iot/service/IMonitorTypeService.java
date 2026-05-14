package com.zwei.iot.service;

import com.zwei.iot.domain.MonitorType;

import java.util.List;

/**
 * 监测类型Service接口
 * <p>
 * 定义监测类型的管理操作，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
public interface IMonitorTypeService {
    /**
     * 分页查询监测类型列表
     *
     * @param monitorType 查询条件
     * @param pageNum     页码
     * @param pageSize    每页数量
     * @return 分页结果
     */
    List<MonitorType> selectMonitorTypePage(MonitorType monitorType, int pageNum, int pageSize);

    /**
     * 查询所有监测类型列表（不分页）
     *
     * @return 所有监测类型列表
     */
    List<MonitorType> selectMonitorTypeAll();

    /**
     * 根据ID查询监测类型详情
     *
     * @param id 监测类型ID
     * @return 监测类型详情
     */
    MonitorType selectMonitorTypeById(Long id);

    /**
     * 根据编码查询监测类型
     *
     * @param code 监测类型编码
     * @return 监测类型详情
     */
    MonitorType selectMonitorTypeByCode(String code);

    /**
     * 新增监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    int insertMonitorType(MonitorType monitorType);

    /**
     * 修改监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    int updateMonitorType(MonitorType monitorType);

    /**
     * 删除监测类型（逻辑删除）
     *
     * @param id 监测类型ID
     * @return 影响行数
     */
    int deleteMonitorTypeById(Long id);

    /**
     * 批量删除监测类型（逻辑删除）
     *
     * @param ids 需要删除的监测类型ID数组
     * @return 影响行数
     */
    int deleteMonitorTypeByIds(Long[] ids);

    /**
     * 校验监测类型编码是否唯一
     *
     * @param monitorType 监测类型信息
     * @return true-唯一，false-已存在
     */
    boolean checkMonitorTypeCodeUnique(MonitorType monitorType);
}
