package com.zwei.iot.monitor.service.impl;

import com.zwei.iot.monitor.domain.MonitorContent;
import com.zwei.iot.monitor.domain.MonitorType;
import com.zwei.iot.monitor.mapper.MonitorTypeMapper;
import com.zwei.iot.monitor.service.IMonitorContentService;
import com.zwei.iot.monitor.service.IMonitorTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监测类型Service实现类
 *
 * @author zwei
 */
@Service
public class MonitorTypeServiceImpl implements IMonitorTypeService {

    private final MonitorTypeMapper monitorTypeMapper;
    private final IMonitorContentService monitorContentService;

    @Autowired
    public MonitorTypeServiceImpl(MonitorTypeMapper monitorTypeMapper,
                                  IMonitorContentService monitorContentService) {
        this.monitorTypeMapper = monitorTypeMapper;
        this.monitorContentService = monitorContentService;
    }

    /**
     * 分页查询监测类型列表
     */
    @Override
    public List<MonitorType> selectMonitorTypePage(MonitorType monitorType, int pageNum, int pageSize) {
        List<MonitorType> monitorTypes = monitorTypeMapper.selectMonitorTypeList(monitorType);
        return monitorTypes;
    }

    /**
     * 查询监测类型列表（不分页），支持按查询条件过滤
     */
    @Override
    public List<MonitorType> selectMonitorTypeList(MonitorType monitorType) {
        return monitorTypeMapper.selectMonitorTypeList(monitorType);
    }

    /**
     * 查询所有监测类型列表（不分页）
     */
    @Override
    @Cacheable(value = "monitorTypeList", key = "'all'")
    public List<MonitorType> selectMonitorTypeAll() {
        List<MonitorType> monitorTypes = monitorTypeMapper.selectMonitorTypeAll();
        return monitorTypes;
    }

    /**
     * 根据ID查询监测类型详情
     */
    @Override
    @Cacheable(value = "monitorType", key = "#id")
    public MonitorType selectMonitorTypeById(Long id) {
        MonitorType monitorType = monitorTypeMapper.selectMonitorTypeById(id);
        if (monitorType == null) {
            return null;
        }
        monitorType.setContents(monitorContentService.selectMonitorContentAll(id));
        return monitorType;
    }

    /**
     * 查询所有监测类型及其内容（批量加载，避免 N+1 查询）。
     * <p>
     * 1 次查询加载所有 monitorType + 1 次查询加载所有 monitorContent，
     * 然后在内存中按 monitorTypeId 分组关联，将 N+1 次 SQL 降为 2 次。
     */
    @Override
    @Cacheable(value = "monitorTypeList", key = "'withContents'")
    public List<MonitorType> selectMonitorTypeAllWithContents() {
        List<MonitorType> monitorTypes = monitorTypeMapper.selectMonitorTypeAll();
        List<MonitorContent> allContents = monitorContentService.selectMonitorContentAll(null);
        Map<Long, List<MonitorContent>> contentsByTypeId = allContents.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getMonitorTypeId() != null ? c.getMonitorTypeId() : 0L));
        for (MonitorType mt : monitorTypes) {
            mt.setContents(contentsByTypeId.getOrDefault(mt.getId(), Collections.emptyList()));
        }
        return monitorTypes;
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
            @CacheEvict(value = "monitorTypeList", allEntries = true),
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true)
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
            @CacheEvict(value = "monitorTypeList", allEntries = true),
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public int deleteMonitorTypeById(Long id) {
        monitorContentService.deleteMonitorContentByMonitorTypeId(id);
        return monitorTypeMapper.deleteMonitorTypeById(id);
    }

    /**
     * 批量删除监测类型（逻辑删除）
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "monitorType", allEntries = true),
            @CacheEvict(value = "monitorTypeList", allEntries = true),
            @CacheEvict(value = "monitorContent", allEntries = true),
            @CacheEvict(value = "monitorContentList", allEntries = true)
    })
    @Transactional(rollbackFor = Exception.class)
    public int deleteMonitorTypeByIds(Long[] ids) {
        if (ids != null && ids.length > 0) {
            monitorContentService.deleteMonitorContentByMonitorTypeIds(ids);
        }
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

    @Override
    public int countAll() {
        return monitorTypeMapper.countAll();
    }
}
