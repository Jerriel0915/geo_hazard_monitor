package com.zwei.iot.service.impl;

import com.zwei.iot.domain.MonitorType;
import com.zwei.iot.mapper.MonitorTypeMapper;
import com.zwei.iot.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监测类型Service实现类
 * <p>
 * 提供监测类型的管理操作实现，包括查询、新增、修改、删除等。
 *
 * @author zwei
 */
@Service
public class MonitorTypeServiceImpl implements IMonitorTypeService {
    /**
     * 注入监测类型Mapper
     */
    private final MonitorTypeMapper monitorTypeMapper;

    @Autowired
    public MonitorTypeServiceImpl(MonitorTypeMapper monitorTypeMapper) {
        this.monitorTypeMapper = monitorTypeMapper;
    }

    /**
     * 分页查询监测类型列表
     *
     * @param monitorType 查询条件
     * @param pageNum     页码
     * @param pageSize    每页数量
     * @return 分页结果
     */
    @Override
    public List<MonitorType> selectMonitorTypePage(MonitorType monitorType, int pageNum, int pageSize) {
        return monitorTypeMapper.selectMonitorTypeList(monitorType);
    }

    /**
     * 查询所有监测类型列表（不分页）
     *
     * @return 所有监测类型列表
     */
    @Override
    public List<MonitorType> selectMonitorTypeAll() {
        return monitorTypeMapper.selectMonitorTypeAll();
    }

    /**
     * 根据ID查询监测类型详情
     *
     * @param id 监测类型ID
     * @return 监测类型详情
     */
    @Override
    public MonitorType selectMonitorTypeById(Long id) {
        return monitorTypeMapper.selectMonitorTypeById(id);
    }

    /**
     * 根据编码查询监测类型
     *
     * @param code 监测类型编码
     * @return 监测类型详情
     */
    @Override
    public MonitorType selectMonitorTypeByCode(String code) {
        return monitorTypeMapper.selectMonitorTypeByCode(code);
    }

    /**
     * 新增监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    @Override
    public int insertMonitorType(MonitorType monitorType) {
        return monitorTypeMapper.insertMonitorType(monitorType);
    }

    /**
     * 修改监测类型
     *
     * @param monitorType 监测类型信息
     * @return 影响行数
     */
    @Override
    public int updateMonitorType(MonitorType monitorType) {
        return monitorTypeMapper.updateMonitorType(monitorType);
    }

    /**
     * 删除监测类型（逻辑删除）
     *
     * @param id 监测类型ID
     * @return 影响行数
     */
    @Override
    public int deleteMonitorTypeById(Long id) {
        return monitorTypeMapper.deleteMonitorTypeById(id);
    }

    /**
     * 批量删除监测类型（逻辑删除）
     *
     * @param ids 需要删除的监测类型ID数组
     * @return 影响行数
     */
    @Override
    public int deleteMonitorTypeByIds(Long[] ids) {
        return monitorTypeMapper.deleteMonitorTypeByIds(ids);
    }

    /**
     * 校验监测类型编码是否唯一
     *
     * @param monitorType 监测类型信息
     * @return true-唯一，false-已存在
     */
    @Override
    public boolean checkMonitorTypeCodeUnique(MonitorType monitorType) {
        Long id = monitorType.getId() == null ? 0L : monitorType.getId();
        MonitorType exist = monitorTypeMapper.checkMonitorTypeCodeUnique(monitorType.getCode(), id);
        return exist == null;
    }
}
