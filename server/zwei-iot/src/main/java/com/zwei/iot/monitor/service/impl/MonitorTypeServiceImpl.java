package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 监测类型Service实现类
 *
 * @author zwei
 */
@Service
public class MonitorTypeServiceImpl implements IMonitorTypeService {

    private final MonitorTypeMapper monitorTypeMapper;

    @Autowired
    public MonitorTypeServiceImpl(MonitorTypeMapper monitorTypeMapper) {
        this.monitorTypeMapper = monitorTypeMapper;
    }

    /**
     * 分页查询监测类型列表
     */
    @Override
    public List<MonitorType> selectMonitorTypePage(MonitorType monitorType, int pageNum, int pageSize) {
        return monitorTypeMapper.selectMonitorTypeList(monitorType);
    }

    /**
     * 查询所有监测类型列表（不分页）
     */
    @Override
    public List<MonitorType> selectMonitorTypeAll() {
        return monitorTypeMapper.selectMonitorTypeAll();
    }

    /**
     * 根据ID查询监测类型详情
     */
    @Override
    @Cacheable(value = "monitorType", key = "#id")
    public MonitorType selectMonitorTypeById(Long id) {
        return monitorTypeMapper.selectMonitorTypeById(id);
    }

    /**
     * 根据编码查询监测类型
     */
    @Override
    public MonitorType selectMonitorTypeByCode(String code) {
        return monitorTypeMapper.selectMonitorTypeByCode(code);
    }

    /**
     * 新增监测类型
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorType", key = "#monitorType.id"),
            @CacheEvict(value = "monitorTypeList", allEntries = true)
    })
    public int insertMonitorType(MonitorType monitorType) {
        return monitorTypeMapper.insertMonitorType(monitorType);
    }

    /**
     * 修改监测类型
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorType", key = "#monitorType.id"),
            @CacheEvict(value = "monitorTypeList", allEntries = true)
    })
    public int updateMonitorType(MonitorType monitorType) {
        return monitorTypeMapper.updateMonitorType(monitorType);
    }

    /**
     * 删除监测类型（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorType", key = "#id"),
            @CacheEvict(value = "monitorTypeList", allEntries = true)
    })
    public int deleteMonitorTypeById(Long id) {
        return monitorTypeMapper.deleteMonitorTypeById(id);
    }

    /**
     * 批量删除监测类型（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorType", allEntries = true),
            @CacheEvict(value = "monitorTypeList", allEntries = true)
    })
    public int deleteMonitorTypeByIds(Long[] ids) {
        return monitorTypeMapper.deleteMonitorTypeByIds(ids);
    }

    /**
     * 校验监测类型编码是否唯一
     */
    @Override
    public boolean checkMonitorTypeCodeUnique(MonitorType monitorType) {
        Long id = monitorType.getId() == null ? 0L : monitorType.getId();
        MonitorType exist = monitorTypeMapper.checkMonitorTypeCodeUnique(monitorType.getCode(), id);
        return exist == null;
    }
}
