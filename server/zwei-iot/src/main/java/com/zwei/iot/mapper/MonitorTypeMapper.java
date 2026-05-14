package com.zwei.iot.mapper;

import com.zwei.iot.domain.MonitorType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 监测类型Mapper接口
 * <p>
 * 提供监测类型的数据库查询操作，包括分页查询、详情查询、
 * 新增、修改、删除等CRUD操作。
 *
 * @author zwei
 */
@Mapper
public interface MonitorTypeMapper {
    /**
     * 分页查询监测类型列表
     *
     * @param monitorType 监测类型查询条件
     * @return 监测类型列表
     */
    List<MonitorType> selectMonitorTypeList(MonitorType monitorType);

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
     * @param code 监测类型编码
     * @return 监测类型信息（null表示唯一）
     */
    MonitorType checkMonitorTypeCodeUnique(String code);
}